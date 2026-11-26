package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.PaymentOutcome;
import fr.soft_labs.eventbyte.ports.in.ConfirmPayment;
import fr.soft_labs.eventbyte.ports.out.BookingUpdatePublisher;
import fr.soft_labs.eventbyte.ports.out.BookingRepository;
import fr.soft_labs.eventbyte.ports.out.PaymentGateway;
import fr.soft_labs.eventbyte.ports.out.ProcessedPayments;
import fr.soft_labs.eventbyte.ports.out.TicketStock;

import java.util.Objects;
import java.util.Optional;

/**
 * Paiement réussi -> confirmed. Un même paiement n'est traité qu'une fois ({@link ProcessedPayments}).
 * <p>
 * Paiement arrivé après l'expiration (décision EB-33) : on tente de reprendre les places ;
 * si elles sont encore libres, la réservation est confirmée, sinon le client est remboursé
 * et la réservation reste expirée. On ne vend donc jamais une place deux fois et on ne garde
 * jamais un paiement sans tickets.
 */
public class ConfirmPaymentUseCase implements ConfirmPayment {

    private final BookingRepository bookings;
    private final TicketStock stock;
    private final PaymentGateway payments;
    private final ProcessedPayments processed;
    private final BookingUpdatePublisher bookingUpdates;

    public ConfirmPaymentUseCase(BookingRepository bookings, TicketStock stock, PaymentGateway payments,
                                 ProcessedPayments processed, BookingUpdatePublisher bookingUpdates) {
        this.bookings = Objects.requireNonNull(bookings);
        this.stock = Objects.requireNonNull(stock);
        this.payments = Objects.requireNonNull(payments);
        this.processed = Objects.requireNonNull(processed);
        this.bookingUpdates = Objects.requireNonNull(bookingUpdates);
    }

    @Override
    public PaymentOutcome paymentSucceeded(String paymentIntentId, String bookingId) {
        Objects.requireNonNull(paymentIntentId, "L'identifiant du paiement est obligatoire");
        if (!processed.claim(paymentIntentId)) {
            return PaymentOutcome.ALREADY_PROCESSED;
        }
        try {
            Optional<Booking> found = bookingId == null ? Optional.empty() : bookings.findById(bookingId);
            if (found.isEmpty()) {
                return PaymentOutcome.UNKNOWN_BOOKING;
            }
            return handle(found.get(), paymentIntentId);
        } catch (RuntimeException e) {
            // Échec technique : on libère le paiement pour que le prochain envoi de Stripe soit retraité
            processed.release(paymentIntentId);
            throw e;
        }
    }

    private PaymentOutcome handle(Booking booking, String paymentIntentId) {
        return switch (booking.status()) {
            case RESERVED, AWAITING_PAYMENT -> {
                Booking confirmed = booking.confirm();
                if (bookings.updateIfStatus(confirmed, booking.status())) {
                    bookingUpdates.publish(confirmed);
                    yield PaymentOutcome.CONFIRMED;   // les places sont déjà tenues
                }
                Booking current = bookings.findById(booking.id())
                        .orElseThrow(() -> new IllegalStateException("Réservation disparue : " + booking.id()));
                yield handle(current, paymentIntentId);
            }
            case CONFIRMED -> PaymentOutcome.ALREADY_PROCESSED;
            case EXPIRED -> confirmLateOrRefund(booking, paymentIntentId);
            // requested / rejected : aucune place n'est tenue et ne le sera ; l'argent doit être rendu
            case REQUESTED, REJECTED -> {
                payments.refund(paymentIntentId);
                yield PaymentOutcome.REFUNDED;
            }
        };
    }

    private PaymentOutcome confirmLateOrRefund(Booking expired, String paymentIntentId) {
        if (stock.tryTake(expired.lines())) {
            try {
                Booking confirmed = expired.confirmLate();
                bookings.save(confirmed);
                bookingUpdates.publish(confirmed);
            } catch (RuntimeException e) {
                stock.giveBack(expired.lines());
                throw e;
            }
            return PaymentOutcome.CONFIRMED_LATE;
        }
        payments.refund(paymentIntentId);
        return PaymentOutcome.REFUNDED;
    }
}

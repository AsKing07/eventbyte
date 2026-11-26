package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import fr.soft_labs.eventbyte.domain.booking.PaymentUnavailableException;
import fr.soft_labs.eventbyte.ports.in.StartPayment;
import fr.soft_labs.eventbyte.ports.out.BookingRepository;
import fr.soft_labs.eventbyte.ports.out.PaymentGateway;
import fr.soft_labs.eventbyte.ports.out.TicketStock;

import java.util.Objects;

/**
 * reserved -> awaiting-payment : crée le PaymentIntent Stripe du montant total (tickets + frais, en centimes)
 * et enregistre son clientSecret. Le montant vient de la réservation, donc des prix de la base.
 * <p>
 * Si Stripe échoue, les places sont rendues et la réservation passe en expired : on ne garde pas
 * de stock bloqué pour une réservation que personne ne pourra payer.
 */
public class StartPaymentUseCase implements StartPayment {

    private final BookingRepository bookings;
    private final PaymentGateway payments;
    private final TicketStock stock;

    public StartPaymentUseCase(BookingRepository bookings, PaymentGateway payments, TicketStock stock) {
        this.bookings = Objects.requireNonNull(bookings);
        this.payments = Objects.requireNonNull(payments);
        this.stock = Objects.requireNonNull(stock);
    }

    @Override
    public Booking startPayment(String bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Réservation inconnue : " + bookingId));
        if (booking.status() == BookingStatus.AWAITING_PAYMENT) {
            return booking;   // déjà fait : pas de second PaymentIntent
        }
        if (booking.status() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Réservation " + bookingId + " en " + booking.status() + " : paiement impossible");
        }

        String clientSecret;
        try {
            clientSecret = payments.createPaymentIntent(booking.id(), booking.totalInCents());
        } catch (RuntimeException e) {
            stock.giveBack(booking.lines());
            bookings.save(booking.expire());
            throw e instanceof PaymentUnavailableException unavailable
                    ? unavailable
                    : new PaymentUnavailableException("Paiement indisponible pour la réservation " + bookingId, e);
        }

        // Si cet enregistrement échoue, la réservation reste reserved : l'expiration (US-14) rendra les places.
        Booking awaiting = booking.awaitPayment(clientSecret);
        bookings.save(awaiting);
        return awaiting;
    }
}

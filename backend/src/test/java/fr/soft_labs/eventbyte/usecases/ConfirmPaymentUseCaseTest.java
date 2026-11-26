package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import fr.soft_labs.eventbyte.domain.booking.PaymentOutcome;
import fr.soft_labs.eventbyte.ports.out.ProcessedPayments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** US-17 : confirmation, doublons et paiement tardif (EB-33), avec Stripe et le stock remplacés par des faux. */
class ConfirmPaymentUseCaseTest {

    static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");
    static final String PI = "pi_123";

    /** Faux registre en mémoire, atomique comme la clé primaire en base. */
    static class InMemoryProcessedPayments implements ProcessedPayments {
        final Set<String> ids = new HashSet<>();

        @Override
        public boolean claim(String paymentIntentId) {
            return ids.add(paymentIntentId);
        }

        @Override
        public void release(String paymentIntentId) {
            ids.remove(paymentIntentId);
        }
    }

    RequestBookingUseCaseTest.RecordingRepository bookings;
    RequestBookingUseCaseTest.FakeStock stock;
    StartPaymentUseCaseTest.FakePaymentGateway payments;
    InMemoryProcessedPayments processed;
    ConfirmPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        bookings = new RequestBookingUseCaseTest.RecordingRepository();
        stock = new RequestBookingUseCaseTest.FakeStock();
        payments = new StartPaymentUseCaseTest.FakePaymentGateway();
        processed = new InMemoryProcessedPayments();
        useCase = new ConfirmPaymentUseCase(bookings, stock, payments, processed, new BookingUpdatesUseCase());
    }

    /** 2 places Standard, déjà en attente de paiement. */
    Booking awaitingPayment() {
        Booking booking = Booking.request("bk_1", new UserId("alice"), "evt_1",
                        List.of(new BookingLine("tkt_std", "Standard", 2, 2200)), NOW)
                .reserve().awaitPayment("pi_123_secret");
        bookings.bookings.put(booking.id(), booking);
        return booking;
    }

    Booking expired() {
        Booking booking = awaitingPayment().expire();
        bookings.bookings.put(booking.id(), booking);
        return booking;
    }

    BookingStatus status() {
        return bookings.bookings.get("bk_1").status();
    }

    @Test
    void un_paiement_reussi_confirme_la_reservation() {
        awaitingPayment();

        assertThat(useCase.paymentSucceeded(PI, "bk_1")).isEqualTo(PaymentOutcome.CONFIRMED);
        assertThat(status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(payments.refunds).isEmpty();
    }

    @Test
    void un_webhook_recu_deux_fois_n_est_traite_qu_une_fois() {
        awaitingPayment();
        useCase.paymentSucceeded(PI, "bk_1");
        int saves = bookings.savedStatuses.size();

        assertThat(useCase.paymentSucceeded(PI, "bk_1")).isEqualTo(PaymentOutcome.ALREADY_PROCESSED);
        assertThat(bookings.savedStatuses).hasSize(saves);
    }

    @Test
    void paiement_apres_expiration_places_encore_libres_la_reservation_est_confirmee() {
        expired();
        stock.available.put("tkt_std", 5);   // les 2 places rendues à l'expiration n'ont pas été revendues

        assertThat(useCase.paymentSucceeded(PI, "bk_1")).isEqualTo(PaymentOutcome.CONFIRMED_LATE);
        assertThat(status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(stock.available).containsEntry("tkt_std", 3);   // places reprises
        assertThat(payments.refunds).isEmpty();
    }

    @Test
    void paiement_apres_expiration_sans_places_le_client_est_rembourse() {
        expired();
        stock.available.put("tkt_std", 1);   // il en faut 2

        assertThat(useCase.paymentSucceeded(PI, "bk_1")).isEqualTo(PaymentOutcome.REFUNDED);
        assertThat(status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(stock.available).containsEntry("tkt_std", 1);   // rien de pris
        assertThat(payments.refunds).containsExactly(PI);
    }

    @Test
    void un_remboursement_n_est_pas_refait_si_le_webhook_revient() {
        expired();
        useCase.paymentSucceeded(PI, "bk_1");
        stock.available.put("tkt_std", 10);   // même si des places se libèrent entre-temps

        assertThat(useCase.paymentSucceeded(PI, "bk_1")).isEqualTo(PaymentOutcome.ALREADY_PROCESSED);
        assertThat(status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(payments.refunds).containsExactly(PI);
    }

    @Test
    void en_cas_d_echec_technique_le_prochain_envoi_de_stripe_est_retraite() {
        awaitingPayment();
        bookings.failOnSave = true;

        assertThatThrownBy(() -> useCase.paymentSucceeded(PI, "bk_1")).isInstanceOf(IllegalStateException.class);
        bookings.failOnSave = false;

        assertThat(useCase.paymentSucceeded(PI, "bk_1")).isEqualTo(PaymentOutcome.CONFIRMED);
    }

    @Test
    void une_reservation_inconnue_est_ignoree() {
        assertThat(useCase.paymentSucceeded(PI, "bk_inconnue")).isEqualTo(PaymentOutcome.UNKNOWN_BOOKING);
        assertThat(useCase.paymentSucceeded("pi_sans_reservation", null)).isEqualTo(PaymentOutcome.UNKNOWN_BOOKING);
        assertThat(payments.refunds).isEmpty();
    }
}

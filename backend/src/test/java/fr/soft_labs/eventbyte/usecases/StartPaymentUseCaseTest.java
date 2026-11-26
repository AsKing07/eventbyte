package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import fr.soft_labs.eventbyte.domain.booking.PaymentUnavailableException;
import fr.soft_labs.eventbyte.ports.out.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** US-16 : le port Stripe est remplacé par un faux qui enregistre les appels. */
class StartPaymentUseCaseTest {

    static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    /** Faux Stripe : renvoie un clientSecret, ou échoue à la demande. */
    static class FakePaymentGateway implements PaymentGateway {
        record Call(String bookingId, long amountInCents) {}

        final List<Call> calls = new ArrayList<>();
        RuntimeException failure;

        @Override
        public String createPaymentIntent(String bookingId, long amountInCents) {
            calls.add(new Call(bookingId, amountInCents));
            if (failure != null) {
                throw failure;
            }
            return "pi_" + bookingId + "_secret_test";
        }
    }

    RequestBookingUseCaseTest.RecordingRepository repository;
    RequestBookingUseCaseTest.FakeStock stock;
    FakePaymentGateway payments;
    StartPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new RequestBookingUseCaseTest.RecordingRepository();
        stock = new RequestBookingUseCaseTest.FakeStock();
        stock.available.putAll(Map.of("tkt_std", 78, "tkt_vip", 0));   // places déjà prises par la réservation
        payments = new FakePaymentGateway();
        useCase = new StartPaymentUseCase(repository, payments, stock);
    }

    /** 2 Standard à 22 € + 1 VIP à 35,50 € : 79,50 € de tickets + 3 x 0,30 € de frais = 80,40 €. */
    Booking reserved() {
        Booking booking = Booking.request("bk_1", new UserId("alice"), "evt_1",
                List.of(new BookingLine("tkt_std", "Standard", 2, 2200), new BookingLine("tkt_vip", "VIP", 1, 3550)),
                NOW).reserve();
        repository.bookings.put(booking.id(), booking);
        return booking;
    }

    @Test
    void cree_le_paiement_du_total_frais_compris_et_passe_en_awaiting_payment() {
        reserved();

        Booking booking = useCase.startPayment("bk_1");

        assertThat(payments.calls).containsExactly(new FakePaymentGateway.Call("bk_1", 8040));
        assertThat(booking.status()).isEqualTo(BookingStatus.AWAITING_PAYMENT);
        assertThat(booking.clientSecret()).isEqualTo("pi_bk_1_secret_test");
        assertThat(booking.feesInCents()).isEqualTo(90);
        assertThat(repository.bookings.get("bk_1")).isEqualTo(booking);
    }

    @Test
    void un_second_appel_ne_cree_pas_un_second_paiement() {
        reserved();
        Booking first = useCase.startPayment("bk_1");

        Booking second = useCase.startPayment("bk_1");

        assertThat(second).isEqualTo(first);
        assertThat(payments.calls).hasSize(1);
    }

    @Test
    void si_stripe_echoue_les_places_sont_rendues_et_la_reservation_expire() {
        reserved();
        payments.failure = new IllegalStateException("Stripe injoignable");

        assertThatThrownBy(() -> useCase.startPayment("bk_1"))
                .isInstanceOf(PaymentUnavailableException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(stock.available).containsEntry("tkt_std", 80).containsEntry("tkt_vip", 1);
        assertThat(repository.bookings.get("bk_1").status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(repository.bookings.get("bk_1").clientSecret()).isNull();
    }

    @Test
    void une_erreur_de_paiement_est_transmise_telle_quelle() {
        reserved();
        payments.failure = new PaymentUnavailableException("STRIPE_SECRET_KEY n'est pas configurée");

        assertThatThrownBy(() -> useCase.startPayment("bk_1"))
                .isInstanceOf(PaymentUnavailableException.class)
                .hasMessageContaining("STRIPE_SECRET_KEY");
    }

    @Test
    void refuse_une_reservation_rejetee_ou_inconnue_sans_appeler_stripe() {
        Booking rejected = Booking.request("bk_2", new UserId("alice"), "evt_1",
                List.of(new BookingLine("tkt_std", "Standard", 1, 2200)), NOW).reject();
        repository.bookings.put(rejected.id(), rejected);

        assertThatThrownBy(() -> useCase.startPayment("bk_2")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> useCase.startPayment("bk_inconnue")).isInstanceOf(IllegalArgumentException.class);
        assertThat(payments.calls).isEmpty();
    }
}

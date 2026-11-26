package fr.soft_labs.eventbyte.domain.booking;

import fr.soft_labs.eventbyte.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    static final UserId ALICE = new UserId("alice");
    static final Instant NOW = Instant.parse("2026-11-25T18:00:00Z");

    static Booking requested(BookingLine... lines) {
        return Booking.request("bk_1", ALICE, "evt_1", List.of(lines), NOW);
    }

    @Test
    void une_nouvelle_reservation_est_requested() {
        assertThat(requested(new BookingLine("a", "Standard", 1, 2200)).status()).isEqualTo(BookingStatus.REQUESTED);
    }

    @Test
    void total_et_frais_de_0_30_euro_par_ticket() {
        Booking booking = requested(new BookingLine("a", "Standard", 2, 2200), new BookingLine("b", "VIP", 1, 3550));

        assertThat(booking.ticketCount()).isEqualTo(3);
        assertThat(booking.feesInCents()).isEqualTo(90);                     // 3 x 0,30 €
        assertThat(booking.totalInCents()).isEqualTo(2 * 2200 + 3550 + 90);  // 80,40 €
    }

    @Test
    void passe_en_reserved_ou_rejected_depuis_requested() {
        assertThat(requested(new BookingLine("a", "S", 1, 100)).reserve().status()).isEqualTo(BookingStatus.RESERVED);
        assertThat(requested(new BookingLine("a", "S", 1, 100)).reject().status()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void une_reservation_rejetee_ne_peut_plus_etre_reservee() {
        Booking rejected = requested(new BookingLine("a", "S", 1, 100)).reject();

        assertThatThrownBy(rejected::reserve).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refuse_une_reservation_vide_ou_un_ticket_en_double() {
        assertThatThrownBy(() -> requested()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> requested(new BookingLine("a", "S", 1, 100), new BookingLine("a", "S", 2, 100)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_une_quantite_nulle_ou_negative() {
        assertThatThrownBy(() -> new BookingLine("a", "S", 0, 100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequestedItem("a", -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sait_a_qui_elle_appartient() {
        Booking booking = requested(new BookingLine("a", "S", 1, 100));

        assertThat(booking.belongsTo(ALICE)).isTrue();
        assertThat(booking.belongsTo(new UserId("bob"))).isFalse();
    }
}

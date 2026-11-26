package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.BookingNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** US-15 : 404 si absente, et jamais la réservation d'un autre utilisateur. */
class ViewBookingsUseCaseTest {

    static final UserId ALICE = new UserId("alice");
    static final UserId BOB = new UserId("bob");

    RequestBookingUseCaseTest.RecordingRepository repository;
    ViewBookingsUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new RequestBookingUseCaseTest.RecordingRepository();
        useCase = new ViewBookingsUseCase(repository);
    }

    Booking booking(String id, UserId user, String bookedAt) {
        Booking booking = Booking.request(id, user, "evt_1",
                List.of(new BookingLine("tkt_std", "Standard", 1, 2200)), Instant.parse(bookedAt)).reserve();
        repository.save(booking);
        return booking;
    }

    @Test
    void l_utilisateur_lit_sa_reservation() {
        Booking mine = booking("bk_1", ALICE, "2026-11-26T10:00:00Z");

        assertThat(useCase.getBooking(ALICE, "bk_1")).isEqualTo(mine);
    }

    @Test
    void une_reservation_inexistante_donne_404() {
        assertThatThrownBy(() -> useCase.getBooking(ALICE, "bk_inconnue"))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void la_reservation_d_un_autre_donne_404_comme_une_absente() {
        booking("bk_alice", ALICE, "2026-11-26T10:00:00Z");

        assertThatThrownBy(() -> useCase.getBooking(BOB, "bk_alice"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage(new BookingNotFoundException("bk_alice").getMessage());
    }

    @Test
    void la_liste_ne_contient_que_mes_reservations_la_plus_recente_d_abord() {
        booking("bk_ancienne", ALICE, "2026-11-20T10:00:00Z");
        booking("bk_bob", BOB, "2026-11-25T10:00:00Z");
        booking("bk_recente", ALICE, "2026-11-26T10:00:00Z");

        assertThat(useCase.getUserBookings(ALICE)).extracting(Booking::id)
                .containsExactly("bk_recente", "bk_ancienne");
        assertThat(useCase.getUserBookings(new UserId("sans-reservation"))).isEmpty();
    }
}

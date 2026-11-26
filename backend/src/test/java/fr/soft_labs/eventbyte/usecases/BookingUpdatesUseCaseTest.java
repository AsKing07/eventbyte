package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingUpdatesUseCaseTest {

    @Test
    void diffuse_les_changements_uniquement_aux_abonnes_de_l_utilisateur_concerne() {
        BookingUpdatesUseCase useCase = new BookingUpdatesUseCase();
        List<Booking> aliceUpdates = new ArrayList<>();
        List<Booking> bobUpdates = new ArrayList<>();
        useCase.subscribe(new UserId("alice"), aliceUpdates::add);
        useCase.subscribe(new UserId("bob"), bobUpdates::add);

        Booking aliceBooking = Booking.request("bk_alice", new UserId("alice"), "event",
                List.of(new BookingLine("ticket", "Standard", 1, 1200)), Instant.parse("2026-11-26T10:00:00Z"))
                .reserve();

        useCase.publish(aliceBooking);

        assertThat(aliceUpdates).containsExactly(aliceBooking);
        assertThat(bobUpdates).isEmpty();
    }

    @Test
    void ferme_une_souscription_sans_affecter_les_autres_abonnes() {
        BookingUpdatesUseCase useCase = new BookingUpdatesUseCase();
        List<Booking> closedListenerUpdates = new ArrayList<>();
        List<Booking> activeListenerUpdates = new ArrayList<>();
        var closedSubscription = useCase.subscribe(new UserId("alice"), closedListenerUpdates::add);
        useCase.subscribe(new UserId("alice"), activeListenerUpdates::add);
        closedSubscription.close();

        Booking booking = Booking.request("bk_alice", new UserId("alice"), "event",
                List.of(new BookingLine("ticket", "Standard", 1, 1200)), Instant.parse("2026-11-26T10:00:00Z"));
        useCase.publish(booking);

        assertThat(closedListenerUpdates).isEmpty();
        assertThat(activeListenerUpdates).containsExactly(booking);
    }
}

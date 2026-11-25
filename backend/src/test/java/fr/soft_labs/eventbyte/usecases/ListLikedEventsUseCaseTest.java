package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListLikedEventsUseCaseTest {

    @Test
    void chaque_utilisateur_ne_voit_que_ses_evenements_likes() {
        UserId sabine = new UserId("sabine");
        UserId charbel = new UserId("charbel");
        EventView concert = evenement("evt_concert", "Concert de jazz");
        EventView theatre = evenement("evt_theatre", "Piece de theatre");

        LikeRepository likes = mock(LikeRepository.class);
        when(likes.findEventIdsByUser(sabine)).thenReturn(List.of(concert.event().id()));
        when(likes.findEventIdsByUser(charbel)).thenReturn(List.of(theatre.event().id()));

        EventReadModel events = ids -> List.of(concert, theatre).stream()
                .filter(view -> ids.contains(view.event().id()))
                .toList();
        ListLikedEventsUseCase listLikedEvents = new ListLikedEventsUseCase(likes, events);

        assertThat(listLikedEvents.listLikedEvents(sabine)).containsExactly(concert);
        assertThat(listLikedEvents.listLikedEvents(charbel)).containsExactly(theatre);
    }

    private static EventView evenement(String id, String titre) {
        Event event = new Event(id, titre, "", "", Instant.parse("2026-12-31T21:00:00Z"),
                new Venue("Salle", "Paris", new Coordinates(48.85, 2.35)), List.of());
        return new EventView(event, 1);
    }
}

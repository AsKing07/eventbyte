package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListLikedEventsUseCaseTest {

    static final UserId SABINE = new UserId("sabine");
    static final UserId CHARBEL = new UserId("charbel");
    static final Instant DATE = Instant.parse("2026-12-31T21:00:00Z");

    InMemoryLikeRepository likes;
    /** Événements visibles (non supprimés) : le faux read model ne renvoie que ceux-là. */
    Map<String, EventView> visibles;
    ListLikedEventsUseCase listLikedEvents;

    @BeforeEach
    void setUp() {
        likes = new InMemoryLikeRepository();
        visibles = new LinkedHashMap<>();
        // Renvoie volontairement dans un ordre différent de celui demandé, comme une base
        EventReadModel events = (Collection<String> ids) -> visibles.values().stream()
                .filter(view -> ids.contains(view.event().id()))
                .sorted((a, b) -> b.event().id().compareTo(a.event().id()))
                .toList();
        listLikedEvents = new ListLikedEventsUseCase(likes, events);
    }

    void visible(String id, Instant startAt) {
        Event event = new Event(id, "Événement " + id, "", "", startAt,
                new Venue("Salle", "Paris", new Coordinates(48.85, 2.35)), List.of());
        visibles.put(id, new EventView(event, 1));
    }

    static List<String> ids(List<EventView> views) {
        return views.stream().map(view -> view.event().id()).toList();
    }

    @Test
    void chaque_utilisateur_ne_voit_que_ses_evenements_likes() {
        visible("evt_concert", DATE);
        visible("evt_theatre", DATE);
        likes.add(SABINE, "evt_concert");
        likes.add(CHARBEL, "evt_theatre");

        assertThat(ids(listLikedEvents.listLikedEvents(SABINE))).containsExactly("evt_concert");
        assertThat(ids(listLikedEvents.listLikedEvents(CHARBEL))).containsExactly("evt_theatre");
    }

    @Test
    void le_like_le_plus_recent_arrive_en_premier() {
        visible("evt_a", DATE);
        visible("evt_b", DATE);
        visible("evt_c", DATE);
        likes.add(SABINE, "evt_b");
        likes.add(SABINE, "evt_a");
        likes.add(SABINE, "evt_c");

        assertThat(ids(listLikedEvents.listLikedEvents(SABINE))).containsExactly("evt_c", "evt_a", "evt_b");
    }

    @Test
    void un_evenement_supprime_n_apparait_plus() {
        visible("evt_visible", DATE);
        likes.add(SABINE, "evt_visible");
        likes.add(SABINE, "evt_supprime");   // liké puis supprimé : le read model ne le renvoie plus

        assertThat(ids(listLikedEvents.listLikedEvents(SABINE))).containsExactly("evt_visible");
    }

    @Test
    void un_evenement_sans_date_est_exclu() {
        visible("evt_date", DATE);
        visible("evt_sans_date", null);
        likes.add(SABINE, "evt_date");
        likes.add(SABINE, "evt_sans_date");

        assertThat(ids(listLikedEvents.listLikedEvents(SABINE))).containsExactly("evt_date");
    }

    @Test
    void sans_like_la_liste_est_vide() {
        assertThat(listLikedEvents.listLikedEvents(SABINE)).isEmpty();
    }
}

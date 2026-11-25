package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.domain.event.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LikeEventUseCaseTest {

    static final UserId ALICE = new UserId("alice");
    static final UserId BOB = new UserId("bob");

    InMemoryEventRepository events;
    InMemoryLikeRepository likes;
    LikeEventUseCase likeEvent;

    @BeforeEach
    void setUp() {
        events = new InMemoryEventRepository();
        likes = new InMemoryLikeRepository();
        likeEvent = new LikeEventUseCase(events, likes);
        events.save(new Event("evt_1", "Concert", "desc", "https://img",
                null, new Venue("Zénith", "Paris", new Coordinates(48.89, 2.39)), List.of()));
    }

    @Test
    void liker_un_evenement_augmente_son_likedCount() {
        likeEvent.like(ALICE, "evt_1");

        assertThat(likes.countFor("evt_1")).isEqualTo(1);
    }

    @Test
    void liker_deux_fois_ne_compte_qu_un_like() {
        likeEvent.like(ALICE, "evt_1");
        likeEvent.like(ALICE, "evt_1");

        assertThat(likes.countFor("evt_1")).isEqualTo(1);
    }

    @Test
    void deux_utilisateurs_differents_font_deux_likes() {
        likeEvent.like(ALICE, "evt_1");
        likeEvent.like(BOB, "evt_1");

        assertThat(likes.countFor("evt_1")).isEqualTo(2);
    }

    @Test
    void liker_un_evenement_inconnu_est_refuse() {
        assertThatThrownBy(() -> likeEvent.like(ALICE, "evt_inconnu"))
                .isInstanceOf(EventNotFoundException.class);
        assertThat(likes.likes).isEmpty();
    }

    @Test
    void liker_un_evenement_supprime_est_refuse() {
        events.markDeleted("evt_1", Instant.parse("2026-11-25T10:00:00Z"));

        assertThatThrownBy(() -> likeEvent.like(ALICE, "evt_1"))
                .isInstanceOf(EventNotFoundException.class);
        assertThat(likes.likes).isEmpty();
    }
}

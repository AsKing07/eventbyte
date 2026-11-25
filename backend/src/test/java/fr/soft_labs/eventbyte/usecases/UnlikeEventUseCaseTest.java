package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnlikeEventUseCaseTest {

    private static final UserId SABINE = new UserId("sabine");
    private static final String EVENT_ID = "evt_1";

    private InMemoryLikeRepository likes;
    private UnlikeEventUseCase unlikeEvent;

    @BeforeEach
    void setUp() {
        likes = new InMemoryLikeRepository();
        unlikeEvent = new UnlikeEventUseCase(likes);
    }

    @Test
    void retirer_mon_like_diminue_le_likedCount() {
        likes.add(SABINE, EVENT_ID);

        unlikeEvent.unlike(SABINE, EVENT_ID);

        assertThat(likes.countFor(EVENT_ID)).isZero();
    }

    @Test
    void retirer_un_like_absent_ne_plante_pas() {
        unlikeEvent.unlike(SABINE, EVENT_ID);

        assertThat(likes.countFor(EVENT_ID)).isZero();
    }

    @Test
    void retirer_deux_fois_reste_a_zero() {
        likes.add(SABINE, EVENT_ID);

        unlikeEvent.unlike(SABINE, EVENT_ID);
        unlikeEvent.unlike(SABINE, EVENT_ID);

        assertThat(likes.countFor(EVENT_ID)).isZero();
    }
}

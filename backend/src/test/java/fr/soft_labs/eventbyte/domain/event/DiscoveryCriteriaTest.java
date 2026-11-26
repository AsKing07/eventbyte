package fr.soft_labs.eventbyte.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscoveryCriteriaTest {

    static final Instant NOW = Instant.parse("2026-12-01T12:00:00Z");
    static final DiscoveryCriteria PARIS = new DiscoveryCriteria(new Coordinates(48.8585, 2.3475), NOW);

    static Event startingAt(Instant startAt) {
        return new Event("evt_1", "Concert", "d", "https://img", startAt,
                new Venue("Le Rex", "Paris", new Coordinates(48.87, 2.34)), List.of());
    }

    @Test
    void le_perimetre_decide_est_50_km_et_50_evenements() {
        assertThat(DiscoveryCriteria.RADIUS_KM).isEqualTo(50);
        assertThat(DiscoveryCriteria.MAX_RESULTS).isEqualTo(50);
    }

    @Test
    void un_evenement_futur_ou_qui_commence_maintenant_est_a_venir() {
        assertThat(PARIS.isUpcoming(startingAt(NOW.plusSeconds(3600)))).isTrue();
        assertThat(PARIS.isUpcoming(startingAt(NOW))).isTrue();
    }

    @Test
    void un_evenement_passe_ou_sans_date_n_est_pas_a_venir() {
        assertThat(PARIS.isUpcoming(startingAt(NOW.minusSeconds(1)))).isFalse();
        assertThat(PARIS.isUpcoming(startingAt(null))).isFalse();
    }

    @Test
    void la_position_et_la_date_sont_obligatoires() {
        assertThatThrownBy(() -> new DiscoveryCriteria(null, NOW)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DiscoveryCriteria(new Coordinates(0, 0), null))
                .isInstanceOf(NullPointerException.class);
    }
}

package fr.soft_labs.eventbyte.domain.event;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchCriteriaTest {

    static final Coordinates PARIS = new Coordinates(48.8566, 2.3522);

    @ParameterizedTest
    @ValueSource(doubles = {0, -5, 500.1, Double.NaN})
    void refuse_un_rayon_invalide(double radius) {
        assertThatThrownBy(() -> new SearchCriteria(PARIS, radius, "jazz")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_mot_cle_absent_ou_blanc_devient_vide() {
        assertThat(new SearchCriteria(PARIS, 10, null).hasQuery()).isFalse();
        assertThat(new SearchCriteria(PARIS, 10, "   ").hasQuery()).isFalse();
        assertThat(new SearchCriteria(PARIS, 10, "  jazz ").query()).isEqualTo("jazz");
    }
}

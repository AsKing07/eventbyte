package fr.soft_labs.eventbyte.adapters.out.search;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.DiscoveryCriteria;
import fr.soft_labs.eventbyte.domain.event.SearchCriteria;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MeilisearchEventIndexTest {

    @Test
    void le_filtre_geographique_est_en_metres_avec_un_point_decimal() {
        Locale avant = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);   // la virgule décimale française ne doit pas fuiter dans le filtre
        try {
            SearchCriteria criteria = new SearchCriteria(new Coordinates(48.8566, 2.3522), 2.5, "jazz");

            assertThat(MeilisearchEventIndex.geoRadius(criteria)).isEqualTo("_geoRadius(48.8566, 2.3522, 2500)");
        } finally {
            Locale.setDefault(avant);
        }
    }

    @Test
    void la_decouverte_filtre_sur_50_km_et_les_evenements_a_venir() {
        Locale avant = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
        try {
            DiscoveryCriteria criteria = new DiscoveryCriteria(new Coordinates(48.8585, 2.3475),
                    Instant.parse("2026-12-01T12:00:00Z"));

            assertThat(MeilisearchEventIndex.discoveryFilter(criteria))
                    .isEqualTo("_geoRadius(48.8585, 2.3475, 50000) AND startAt >= 1796126400");
        } finally {
            Locale.setDefault(avant);
        }
    }
}

package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.SearchCriteria;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SearchEventsUseCaseTest {

    static final SearchCriteria JAZZ_A_PARIS = new SearchCriteria(new Coordinates(48.85, 2.35), 10, "jazz");

    InMemoryEventSearchIndex index;
    Map<String, EventView> database;
    SearchEventsUseCase search;

    @BeforeEach
    void setUp() {
        index = new InMemoryEventSearchIndex();
        database = new HashMap<>();
        // Faux read model : renvoie volontairement dans un ordre quelconque, comme une base
        EventReadModel readModel = (Collection<String> ids) ->
                database.values().stream().filter(v -> ids.contains(v.event().id())).toList();
        search = new SearchEventsUseCase(index, readModel);
    }

    void inDatabase(String id, Instant startAt) {
        Event event = new Event(id, "Soirée " + id, "d", "https://img", startAt,
                new Venue("Le Rex", "Paris", new Coordinates(48.87, 2.34)), List.of());
        database.put(id, new EventView(event, 0));
    }

    static List<String> ids(List<EventView> views) {
        return views.stream().map(v -> v.event().id()).toList();
    }

    @Test
    void transmet_les_criteres_a_l_index() {
        search.search(JAZZ_A_PARIS);

        assertThat(index.lastCriteria).isEqualTo(JAZZ_A_PARIS);
    }

    @Test
    void garde_l_ordre_de_pertinence_de_l_index() {
        Instant date = Instant.parse("2026-12-31T21:00:00Z");
        inDatabase("evt_a", date);
        inDatabase("evt_b", date);
        inDatabase("evt_c", date);
        index.nextResults = List.of("evt_c", "evt_a", "evt_b");

        assertThat(ids(search.search(JAZZ_A_PARIS))).containsExactly("evt_c", "evt_a", "evt_b");
    }

    @Test
    void ignore_les_ids_que_la_base_ne_renvoie_pas() {
        inDatabase("evt_a", Instant.parse("2026-12-31T21:00:00Z"));
        index.nextResults = List.of("evt_supprime", "evt_a");

        assertThat(ids(search.search(JAZZ_A_PARIS))).containsExactly("evt_a");
    }

    @Test
    void exclut_les_evenements_sans_date_que_le_front_ne_sait_pas_afficher() {
        inDatabase("evt_date", Instant.parse("2026-12-31T21:00:00Z"));
        inDatabase("evt_sans_date", null);
        index.nextResults = List.of("evt_sans_date", "evt_date");

        assertThat(ids(search.search(JAZZ_A_PARIS))).containsExactly("evt_date");
    }

    @Test
    void aucun_resultat_dans_l_index_donne_une_liste_vide() {
        assertThat(search.search(JAZZ_A_PARIS)).isEmpty();
    }
}

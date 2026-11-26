package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.DiscoveryCriteria;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoverEventsUseCaseTest {

    static final Instant NOW = Instant.parse("2026-12-01T12:00:00Z");
    static final Instant TOMORROW = NOW.plusSeconds(86_400);
    static final Coordinates PARIS = new Coordinates(48.8585, 2.3475);

    InMemoryEventSearchIndex index;
    Map<String, EventView> database;
    DiscoverEventsUseCase discover;

    @BeforeEach
    void setUp() {
        index = new InMemoryEventSearchIndex();
        database = new HashMap<>();
        // Faux read model : renvoie volontairement dans un ordre quelconque, comme une base
        EventReadModel readModel = (Collection<String> ids) ->
                database.values().stream().filter(v -> ids.contains(v.event().id())).toList();
        discover = new DiscoverEventsUseCase(index, readModel, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    void inDatabase(String id, Instant startAt, Ticket... tickets) {
        Event event = new Event(id, "Soirée " + id, "d", "https://img", startAt,
                new Venue("Le Rex", "Paris", new Coordinates(48.87, 2.34)), List.of(tickets));
        database.put(id, new EventView(event, 0));
    }

    static List<String> ids(List<EventView> views) {
        return views.stream().map(v -> v.event().id()).toList();
    }

    @Test
    void cherche_autour_de_la_position_a_partir_de_maintenant() {
        discover.discover(PARIS);

        assertThat(index.lastDiscovery).isEqualTo(new DiscoveryCriteria(PARIS, NOW));
    }

    @Test
    void garde_l_ordre_chronologique_de_l_index() {
        inDatabase("evt_a", TOMORROW);
        inDatabase("evt_b", TOMORROW);
        inDatabase("evt_c", TOMORROW);
        index.nextResults = List.of("evt_c", "evt_a", "evt_b");

        assertThat(ids(discover.discover(PARIS))).containsExactly("evt_c", "evt_a", "evt_b");
    }

    @Test
    void ignore_les_ids_que_la_base_ne_renvoie_pas() {
        inDatabase("evt_a", TOMORROW);
        index.nextResults = List.of("evt_supprime", "evt_a");

        assertThat(ids(discover.discover(PARIS))).containsExactly("evt_a");
    }

    @Test
    void exclut_les_evenements_passes_ou_sans_date_selon_la_base() {
        inDatabase("evt_futur", TOMORROW);
        inDatabase("evt_passe", NOW.minusSeconds(60));   // date modifiée depuis l'indexation
        inDatabase("evt_sans_date", null);
        index.nextResults = List.of("evt_passe", "evt_sans_date", "evt_futur");

        assertThat(ids(discover.discover(PARIS))).containsExactly("evt_futur");
    }

    @Test
    void expose_les_champs_calcules_du_contrat() {
        inDatabase("evt_dispo", TOMORROW, new Ticket("t1", "Early", 1500, 0), new Ticket("t2", "Standard", 2500, 3));
        inDatabase("evt_complet", TOMORROW, new Ticket("t3", "Unique", 2000, 0));
        index.nextResults = List.of("evt_dispo", "evt_complet");

        List<EventView> result = discover.discover(PARIS);

        assertThat(result.get(0).soldOut()).isFalse();
        assertThat(result.get(0).lowestPriceInCents()).isEqualTo(1500);
        assertThat(result.get(0).event().startAt()).isEqualTo(TOMORROW);
        assertThat(result.get(1).soldOut()).isTrue();
    }

    @Test
    void aucun_resultat_dans_l_index_donne_une_liste_vide() {
        assertThat(discover.discover(PARIS)).isEmpty();
    }
}

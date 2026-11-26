package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class GetEventsByIdsUseCaseTest {

    static final Instant DATE = Instant.parse("2026-12-31T21:00:00Z");

    /** Faux read model en mémoire : événements visibles, et trace des lectures demandées. */
    static class FakeEventReadModel implements EventReadModel {

        final Map<String, EventView> visibles = new LinkedHashMap<>();
        final List<List<String>> reads = new ArrayList<>();

        void add(String id, Instant startAt) {
            visibles.put(id, new EventView(new Event(id, "Event " + id, "Description", "https://img", startAt,
                    new Venue("Venue", "Address", new Coordinates(48.85, 2.35)), List.of()), 0));
        }

        @Override
        public List<EventView> findVisibleByIds(Collection<String> ids) {
            reads.add(List.copyOf(ids));
            // Ordre volontairement différent de la demande, comme une base de données
            return visibles.values().stream().filter(view -> ids.contains(view.event().id())).toList();
        }
    }

    FakeEventReadModel readModel;
    GetEventsByIdsUseCase useCase;

    @BeforeEach
    void setUp() {
        readModel = new FakeEventReadModel();
        useCase = new GetEventsByIdsUseCase(readModel);
    }

    static List<String> ids(List<EventView> views) {
        return views.stream().map(view -> view.event().id()).toList();
    }

    @Test
    void conserve_l_ordre_demande_et_ignore_les_evenements_absents_ou_sans_date() {
        readModel.add("evt_a", DATE);
        readModel.add("evt_no_date", null);
        readModel.add("evt_b", DATE);

        assertThat(ids(useCase.getEventsByIds(List.of("evt_b", "evt_missing", "evt_a", "evt_no_date"))))
                .containsExactly("evt_b", "evt_a");
    }

    @Test
    void une_liste_vide_ne_lance_pas_de_lecture() {
        assertThat(useCase.getEventsByIds(List.of())).isEmpty();
        assertThat(readModel.reads).isEmpty();
    }

    @Test
    void dedoublonne_les_ids_avant_la_lecture() {
        readModel.add("evt_a", DATE);

        assertThat(ids(useCase.getEventsByIds(List.of("evt_a", "evt_a")))).containsExactly("evt_a");
        assertThat(readModel.reads).containsExactly(List.of("evt_a"));
    }

    @Test
    void refuse_plus_de_100_ids_sans_lire_la_base() {
        List<String> tooMany = IntStream.rangeClosed(1, GetEventsByIdsUseCase.MAX_IDS + 1)
                .mapToObj(i -> "evt_" + i).toList();

        assertThatIllegalArgumentException().isThrownBy(() -> useCase.getEventsByIds(tooMany));
        assertThat(readModel.reads).isEmpty();
    }

    @Test
    void accepte_exactement_100_ids() {
        List<String> max = IntStream.rangeClosed(1, GetEventsByIdsUseCase.MAX_IDS)
                .mapToObj(i -> "evt_" + i).toList();

        assertThat(useCase.getEventsByIds(max)).isEmpty();
        assertThat(readModel.reads).hasSize(1);
    }
}

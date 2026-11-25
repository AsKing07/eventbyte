package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetEventsByIdsUseCaseTest {

    EventReadModel readModel;
    GetEventsByIdsUseCase useCase;

    @BeforeEach
    void setUp() {
        readModel = mock(EventReadModel.class);
        useCase = new GetEventsByIdsUseCase(readModel);
    }

    private static EventView event(String id, Instant startAt) {
        return new EventView(new Event(id, "Event " + id, "Description", "https://img", startAt,
                new Venue("Venue", "Address", new Coordinates(48.85, 2.35)), List.of()), 0);
    }

    @Test
    void conserve_l_ordre_demande_et_ignore_les_evenements_absents_ou_sans_date() {
        when(readModel.findVisibleByIds(List.of("evt_b", "evt_missing", "evt_a", "evt_no_date")))
                .thenReturn(List.of(
                        event("evt_a", Instant.parse("2026-12-31T21:00:00Z")),
                        event("evt_no_date", null),
                        event("evt_b", Instant.parse("2026-12-31T21:00:00Z"))));

        assertThat(useCase.getEventsByIds(List.of("evt_b", "evt_missing", "evt_a", "evt_no_date")))
                .extracting(view -> view.event().id())
                .containsExactly("evt_b", "evt_a");
    }

    @Test
    void une_liste_vide_ne_lance_pas_de_lecture() {
        assertThat(useCase.getEventsByIds(List.of())).isEmpty();
    }

    @Test
    void dedoublonne_les_ids_avant_la_lecture() {
        when(readModel.findVisibleByIds(List.of("evt_a")))
                .thenReturn(List.of(event("evt_a", Instant.parse("2026-12-31T21:00:00Z"))));

        assertThat(useCase.getEventsByIds(List.of("evt_a", "evt_a")))
                .extracting(view -> view.event().id())
                .containsExactly("evt_a");
        verify(readModel).findVisibleByIds(List.of("evt_a"));
    }
}

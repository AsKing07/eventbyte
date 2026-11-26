package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import org.junit.jupiter.api.Test;

import static fr.soft_labs.eventbyte.usecases.GetEventsByIdsUseCaseTest.DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** US-5 : le faux read model ne renvoie que les événements visibles (non supprimés), comme la base. */
class GetEventByIdUseCaseTest {

    private final GetEventsByIdsUseCaseTest.FakeEventReadModel readModel =
            new GetEventsByIdsUseCaseTest.FakeEventReadModel();
    private final GetEventByIdUseCase useCase = new GetEventByIdUseCase(readModel);

    @Test
    void renvoie_le_detail_de_l_evenement_visible() {
        readModel.add("evt_1", DATE);

        assertThat(useCase.getEventById("evt_1")).isEqualTo(readModel.visibles.get("evt_1"));
    }

    @Test
    void signale_un_evenement_introuvable_ou_supprime() {
        assertThatExceptionOfType(EventNotFoundException.class)
                .isThrownBy(() -> useCase.getEventById("evt_missing"))
                .withMessageContaining("evt_missing");
    }

    @Test
    void ne_renvoie_pas_d_evenement_sans_date() {
        readModel.add("evt_no_date", null);

        assertThatExceptionOfType(EventNotFoundException.class)
                .isThrownBy(() -> useCase.getEventById("evt_no_date"));
    }

    @Test
    void lit_un_seul_evenement() {
        readModel.add("evt_1", DATE);

        useCase.getEventById("evt_1");

        assertThat(readModel.reads).containsExactly(java.util.List.of("evt_1"));
    }
}

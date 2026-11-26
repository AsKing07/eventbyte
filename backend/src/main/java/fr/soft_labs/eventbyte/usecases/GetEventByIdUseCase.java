package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.ports.in.GetEventById;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;

import java.util.List;
import java.util.Objects;

/**
 * Lit l'événement dans le read model, qui écarte déjà les supprimés.
 * Un événement sans date est traité comme introuvable : le front exige startAt (zod).
 */
public class GetEventByIdUseCase implements GetEventById {

    private final EventReadModel events;

    public GetEventByIdUseCase(EventReadModel events) {
        this.events = Objects.requireNonNull(events);
    }

    @Override
    public EventView getEventById(String id) {
        return events.findVisibleByIds(List.of(id)).stream()
                .filter(view -> view.event().startAt() != null)
                .findFirst()
                .orElseThrow(() -> new EventNotFoundException(id));
    }
}

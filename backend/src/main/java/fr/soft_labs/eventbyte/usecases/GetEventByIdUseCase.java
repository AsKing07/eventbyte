package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.ports.in.GetEventById;
import fr.soft_labs.eventbyte.ports.in.GetEventsByIds;

import java.util.List;

public class GetEventByIdUseCase implements GetEventById {

    private final GetEventsByIds getEventsByIds;

    public GetEventByIdUseCase(GetEventsByIds getEventsByIds) {
        this.getEventsByIds = getEventsByIds;
    }

    @Override
    public EventView getEventById(String id) {
        return getEventsByIds.getEventsByIds(List.of(id)).stream()
                .findFirst()
                .orElseThrow(() -> new EventNotFoundException(id));
    }
}

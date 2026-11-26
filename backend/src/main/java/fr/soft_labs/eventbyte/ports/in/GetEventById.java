package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.event.EventView;

public interface GetEventById {

    EventView getEventById(String id);
}

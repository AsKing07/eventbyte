package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.event.EventView;

/** US-5 (EB-11) : consulter un événement public. */
public interface GetEventById {

    /** @throws fr.soft_labs.eventbyte.domain.event.EventNotFoundException si absent, supprimé ou sans date */
    EventView getEventById(String id);
}

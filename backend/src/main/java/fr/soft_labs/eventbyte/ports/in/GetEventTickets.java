package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.event.EventTicketAvailability;

import java.util.List;

/** Consulte les types de tickets et leur disponibilité pour un événement public. */
public interface GetEventTickets {

    List<EventTicketAvailability> getForEvent(String eventId);
}

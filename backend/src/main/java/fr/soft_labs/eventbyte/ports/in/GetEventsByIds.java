package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.event.EventView;

import java.util.Collection;
import java.util.List;

/** Récupère les événements visibles correspondant à une liste d'identifiants. */
public interface GetEventsByIds {

    List<EventView> getEventsByIds(Collection<String> ids);
}

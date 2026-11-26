package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.EventView;

import java.util.List;

/** US-4 : découvrir les événements à venir près d'une position. */
public interface DiscoverEvents {

    /** Événements à venir autour de la position, triés par date de début. */
    List<EventView> discover(Coordinates position);
}

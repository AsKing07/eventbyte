package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.event.Event;

/**
 * répercuter les événements créés, modifiés ou supprimés dans Strapi.
 */
public interface IngestEvent {

    /** event.created et event.updated : le message contient l'état complet de l'événement. */
    void upsert(Event event);

    /** event.deleted : suppression logique, l'événement n'est plus visible du public. */
    void delete(String eventId);
}

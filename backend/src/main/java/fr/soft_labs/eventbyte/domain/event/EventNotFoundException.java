package fr.soft_labs.eventbyte.domain.event;

/**
 * L'événement demandé n'existe pas ou a été supprimé : il n'est pas visible du public.
 */
public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(String eventId) {
        super("Événement introuvable : " + eventId);
    }
}

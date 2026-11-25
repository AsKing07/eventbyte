package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.event.Event;

import java.time.Instant;

public interface EventRepository {

    /** Crée l'événement ou remplace entièrement son état (champs, lieu, tickets). */
    void save(Event event);

    /** Vrai si l'événement existe et a été supprimé. */
    boolean isDeleted(String eventId);

    /** Vrai si l'événement existe et n'est pas supprimé (visible du public). */
    boolean isVisible(String eventId);

    /**
     * Marque l'événement comme supprimé à la date donnée, s'il ne l'est pas déjà.
     *
     * @return faux si l'événement est inconnu
     */
    boolean markDeleted(String eventId, Instant deletedAt);
}

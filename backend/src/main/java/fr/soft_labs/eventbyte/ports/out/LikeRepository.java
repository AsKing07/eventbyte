package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.UserId;

public interface LikeRepository {

    /** Enregistre le like s'il n'existe pas déjà (sans erreur ni doublon sinon). */
    void add(UserId userId, String eventId);

    /** Nombre de likes de l'événement : la valeur de {@code likedCount}. */
    long countFor(String eventId);

    /** Nombre de likes par événement, en une seule requête ; les événements sans like sont absents. */
    java.util.Map<String, Long> countByEvent(java.util.Collection<String> eventIds);
}

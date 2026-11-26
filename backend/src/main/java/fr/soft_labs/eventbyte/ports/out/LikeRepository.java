package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.UserId;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface LikeRepository {

    /** Enregistre le like s'il n'existe pas déjà (sans erreur ni doublon sinon). */
    void add(UserId userId, String eventId);

    /** Retire le like de l'utilisateur ; sans effet s'il n'existe pas. */
    void remove(UserId userId, String eventId);

    /** Événements likés par l'utilisateur, du like le plus récent au plus ancien. */
    List<String> findEventIdsByUser(UserId userId);

    /** Nombre de likes de l'événement : la valeur de {@code likedCount}. */
    long countFor(String eventId);

    /** Nombre de likes par événement, en une seule requête ; les événements sans like sont absents. */
    Map<String, Long> countByEvent(Collection<String> eventIds);
}

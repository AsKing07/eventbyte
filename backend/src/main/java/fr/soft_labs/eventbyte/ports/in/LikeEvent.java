package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.UserId;

/**
 * US-8 (EB-14) : un utilisateur connecté like un événement pour le retrouver plus tard.
 */
public interface LikeEvent {

    /**
     * Idempotent : liker deux fois le même événement ne compte qu'un like.
     *
     * @throws fr.soft_labs.eventbyte.domain.event.EventNotFoundException si l'événement est inconnu ou supprimé
     */
    void like(UserId userId, String eventId);
}

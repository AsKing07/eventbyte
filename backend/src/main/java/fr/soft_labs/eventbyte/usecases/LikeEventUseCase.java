package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.ports.in.LikeEvent;
import fr.soft_labs.eventbyte.ports.out.EventRepository;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;

import java.util.Objects;

/**
 * Règles du like (US-8, EB-14) :
 * <ul>
 *   <li>on ne like qu'un événement visible (ni inconnu, ni supprimé) ;</li>
 *   <li>liker deux fois = un seul like ;</li>
 *   <li>likedCount se déduit des likes enregistrés.</li>
 * </ul>
 */
public class LikeEventUseCase implements LikeEvent {

    private final EventRepository events;
    private final LikeRepository likes;

    public LikeEventUseCase(EventRepository events, LikeRepository likes) {
        this.events = Objects.requireNonNull(events);
        this.likes = Objects.requireNonNull(likes);
    }

    @Override
    public void like(UserId userId, String eventId) {
        Objects.requireNonNull(userId, "L'utilisateur est obligatoire");
        if (!events.isVisible(eventId)) {
            throw new EventNotFoundException(eventId);
        }
        likes.add(userId, eventId);
    }
}

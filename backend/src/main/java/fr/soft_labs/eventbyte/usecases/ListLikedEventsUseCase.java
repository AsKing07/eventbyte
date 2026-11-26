package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.ports.in.ListLikedEvents;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Les likes donnent les ids et l'ordre (plus récent d'abord) ; le read model fournit les données
 * à jour et écarte les événements supprimés. Les événements sans date sont exclus :
 * le front valide startAt avec zod et une seule valeur nulle ferait échouer toute la liste.
 */
public class ListLikedEventsUseCase implements ListLikedEvents {

    private final LikeRepository likes;
    private final EventReadModel events;

    public ListLikedEventsUseCase(LikeRepository likes, EventReadModel events) {
        this.likes = Objects.requireNonNull(likes);
        this.events = Objects.requireNonNull(events);
    }

    @Override
    public List<EventView> listLikedEvents(UserId userId) {
        Objects.requireNonNull(userId, "L'utilisateur est obligatoire");
        List<String> ids = likes.findEventIdsByUser(userId);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            rank.putIfAbsent(ids.get(i), i);
        }
        // findVisibleByIds ne garantit pas l'ordre : on remet celui des likes
        return events.findVisibleByIds(ids).stream()
                .filter(view -> view.event().startAt() != null)
                .sorted(Comparator.comparingInt(view -> rank.getOrDefault(view.event().id(), Integer.MAX_VALUE)))
                .toList();
    }
}

package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.ports.in.GetEventsByIds;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Charge en une lecture les événements visibles et conserve l'ordre des IDs demandés. */
public class GetEventsByIdsUseCase implements GetEventsByIds {

    /** Au-delà, la requête SQL (IN ...) devient démesurée : on refuse plutôt que de tout charger. */
    public static final int MAX_IDS = 100;

    private final EventReadModel events;

    public GetEventsByIdsUseCase(EventReadModel events) {
        this.events = Objects.requireNonNull(events);
    }

    @Override
    public List<EventView> getEventsByIds(Collection<String> ids) {
        List<String> requestedIds = ids.stream().distinct().toList();
        if (requestedIds.isEmpty()) {
            return List.of();
        }
        if (requestedIds.size() > MAX_IDS) {
            throw new IllegalArgumentException("Au plus " + MAX_IDS + " événements par requête");
        }

        Map<String, EventView> eventsById = new HashMap<>();
        for (EventView view : events.findVisibleByIds(requestedIds)) {
            eventsById.put(view.event().id(), view);
        }

        return requestedIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                // Le frontend exige startAt ; les événements sans date ne peuvent pas être affichés.
                .filter(view -> view.event().startAt() != null)
                .toList();
    }
}

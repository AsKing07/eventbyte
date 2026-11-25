package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.ports.out.EventRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Faux repository en mémoire : teste les cas d'usage sans base de données. */
class InMemoryEventRepository implements EventRepository {

    final Map<String, Event> events = new HashMap<>();
    final Map<String, Instant> deletedAt = new HashMap<>();
    int saves;

    @Override
    public void save(Event event) {
        events.put(event.id(), event);
        saves++;
    }

    @Override
    public boolean isDeleted(String eventId) {
        return deletedAt.containsKey(eventId);
    }

    @Override
    public boolean isVisible(String eventId) {
        return events.containsKey(eventId) && !deletedAt.containsKey(eventId);
    }

    @Override
    public boolean markDeleted(String eventId, Instant at) {
        if (!events.containsKey(eventId)) {
            return false;
        }
        deletedAt.putIfAbsent(eventId, at);
        return true;
    }

    Optional<Event> find(String id) {
        return Optional.ofNullable(events.get(id));
    }
}

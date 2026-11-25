package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Component
class JpaEventReadModel implements EventReadModel {

    private final EventJpaRepository jpa;

    JpaEventReadModel(EventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventView> findVisibleByIds(Collection<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpa.findVisibleWithTickets(ids).stream()
                // likedCount : 0 tant que les likes (US-8) n'existent pas ; à remplacer par un comptage
                .map(entity -> new EventView(toDomain(entity), 0))
                .toList();
    }

    static Event toDomain(EventEntity entity) {
        return new Event(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCover(),
                entity.getStartAt(),
                new Venue(entity.getVenueName(), entity.getVenueAddress(),
                        new Coordinates(entity.getVenueLatitude(), entity.getVenueLongitude())),
                entity.getTickets().stream()
                        .map(t -> new Ticket(t.getId(), t.getName(), t.getPrice(), t.getQuantity()))
                        .toList());
    }
}

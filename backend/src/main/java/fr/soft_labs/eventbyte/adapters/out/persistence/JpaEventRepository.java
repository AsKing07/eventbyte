package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.ports.out.EventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class JpaEventRepository implements EventRepository {

    private final EventJpaRepository jpa;
    private final Clock clock;

    JpaEventRepository(EventJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void save(Event event) {
        EventEntity entity = jpa.findById(event.id()).orElseGet(() -> new EventEntity(event.id()));
        entity.setTitle(event.title());
        entity.setDescription(event.description());
        entity.setCover(event.cover());
        entity.setStartAt(event.startAt());
        entity.setVenueName(event.venue().name());
        entity.setVenueAddress(event.venue().address());
        entity.setVenueLatitude(event.venue().coordinates().latitude());
        entity.setVenueLongitude(event.venue().coordinates().longitude());
        entity.setUpdatedAt(clock.instant());
        mergeTickets(entity, event);
        jpa.save(entity);
    }

    /**
     * Met à jour les tickets existants par id plutôt que tout supprimer puis recréer :
     * Hibernate insère avant de supprimer, ce qui violerait la clé primaire.
     */
    private static void mergeTickets(EventEntity entity, Event event) {
        Map<String, Ticket> incoming = event.tickets().stream()
                .collect(Collectors.toMap(Ticket::id, Function.identity()));

        entity.getTickets().removeIf(existing -> !incoming.containsKey(existing.getId()));

        Map<String, TicketEntity> existing = entity.getTickets().stream()
                .collect(Collectors.toMap(TicketEntity::getId, Function.identity()));

        for (Ticket ticket : event.tickets()) {
            TicketEntity target = existing.get(ticket.id());
            if (target == null) {
                target = new TicketEntity(ticket.id(), entity);
                entity.getTickets().add(target);
            }
            target.setName(ticket.name());
            target.setPrice(ticket.priceInCents());
            target.setQuantity(ticket.quantity());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDeleted(String eventId) {
        return jpa.findById(eventId).map(e -> e.getDeletedAt() != null).orElse(false);
    }

    @Override
    public boolean isVisible(String eventId) {
        return jpa.findById(eventId).map(e -> e.getDeletedAt() == null).orElse(false);
    }

    @Override
    @Transactional
    public boolean markDeleted(String eventId, Instant deletedAt) {
        return jpa.findById(eventId).map(entity -> {
            if (entity.getDeletedAt() == null) {
                entity.setDeletedAt(deletedAt);
                entity.setUpdatedAt(clock.instant());
            }
            return true;
        }).orElse(false);
    }
}

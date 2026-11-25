package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.ports.in.IngestEvent;
import fr.soft_labs.eventbyte.ports.out.EventRepository;

import java.time.Clock;
import java.util.Objects;

import static java.lang.System.Logger.Level.INFO;

/**
 * Règles d'ingestion (décisions EB-29 et EB-31) :
 * <ul>
 *   <li>created / updated : upsert par id Strapi, le message fait foi pour tout l'état de l'événement ;</li>
 *   <li>deleted : suppression logique ; un id inconnu est ignoré ;</li>
 *   <li>une fois supprimé, un événement ne revient pas, même si un ancien created/updated arrive après
 *       (l'outbox Strapi ne garantit pas l'ordre des messages).</li>
 * </ul>
 */
public class IngestEventUseCase implements IngestEvent {

    private static final System.Logger LOG = System.getLogger(IngestEventUseCase.class.getName());

    private final EventRepository events;
    private final Clock clock;

    public IngestEventUseCase(EventRepository events, Clock clock) {
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void upsert(Event event) {
        if (events.isDeleted(event.id())) {
            LOG.log(INFO, "Événement {0} déjà supprimé : message ignoré", event.id());
            return;
        }
        events.save(event);
    }

    @Override
    public void delete(String eventId) {
        if (!events.markDeleted(eventId, clock.instant())) {
            LOG.log(INFO, "Suppression d''un événement inconnu {0} : ignorée", eventId);
        }
    }
}

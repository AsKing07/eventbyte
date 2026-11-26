package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.ports.in.IngestEvent;
import fr.soft_labs.eventbyte.ports.out.EventRepository;
import fr.soft_labs.eventbyte.ports.out.EventSearchIndex;
import fr.soft_labs.eventbyte.ports.out.TicketStock;

import java.time.Clock;
import java.util.Objects;

import static java.lang.System.Logger.Level.INFO;

/**
 * Règles d'ingestion (décisions EB-29 et EB-31) :
 * <ul>
 *   <li>created / updated : upsert par id Strapi, le message fait foi pour tout l'état de l'événement ;</li>
 *   <li>deleted : suppression logique ; un id inconnu est ignoré ;</li>
 *   <li>une fois supprimé, un événement ne revient pas, même si un ancien created/updated arrive après
 *       (l'outbox Strapi ne garantit pas l'ordre des messages) ;</li>
 *   <li>US-7 : l'index de recherche est mis à jour à chaque message, après la base.
 *       Si l'index échoue, le message est retenté (opérations idempotentes) puis envoyé en DLQ.</li>
 * </ul>
 */
public class IngestEventUseCase implements IngestEvent {

    private static final System.Logger LOG = System.getLogger(IngestEventUseCase.class.getName());

    private final EventRepository events;
    private final EventSearchIndex searchIndex;
    private final TicketStock stock;
    private final Clock clock;

    public IngestEventUseCase(EventRepository events, EventSearchIndex searchIndex, TicketStock stock, Clock clock) {
        this.events = Objects.requireNonNull(events);
        this.searchIndex = Objects.requireNonNull(searchIndex);
        this.stock = Objects.requireNonNull(stock);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void upsert(Event event) {
        if (events.isDeleted(event.id())) {
            LOG.log(INFO, "Événement {0} déjà supprimé : message ignoré", event.id());
            return;
        }
        events.save(event);
        searchIndex.index(event);
        // US-13 / EB-30 : la quantité a pu changer dans Strapi, le stock disponible sera recalculé
        stock.refresh(event.tickets().stream().map(Ticket::id).toList());
    }

    @Override
    public void delete(String eventId) {
        if (!events.markDeleted(eventId, clock.instant())) {
            LOG.log(INFO, "Suppression d''un événement inconnu {0} : ignorée", eventId);
        }
        // Retiré de l'index dans tous les cas : sans effet s'il n'y était pas
        searchIndex.remove(eventId);
    }
}

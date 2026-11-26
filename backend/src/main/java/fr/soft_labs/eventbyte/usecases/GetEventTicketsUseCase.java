package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.domain.event.EventTicketAvailability;
import fr.soft_labs.eventbyte.ports.in.GetEventTickets;
import fr.soft_labs.eventbyte.ports.out.EventRepository;
import fr.soft_labs.eventbyte.ports.out.TicketCatalog;
import fr.soft_labs.eventbyte.ports.out.TicketStock;

import java.util.List;
import java.util.Objects;

/** Lit les offres d'un événement visible et leur disponibilité réelle côté stock. */
public class GetEventTicketsUseCase implements GetEventTickets {

    private final EventRepository events;
    private final TicketCatalog catalog;
    private final TicketStock stock;

    public GetEventTicketsUseCase(EventRepository events, TicketCatalog catalog, TicketStock stock) {
        this.events = Objects.requireNonNull(events);
        this.catalog = Objects.requireNonNull(catalog);
        this.stock = Objects.requireNonNull(stock);
    }

    @Override
    public List<EventTicketAvailability> getForEvent(String eventId) {
        if (!events.isVisible(eventId)) {
            throw new EventNotFoundException(eventId);
        }
        return catalog.findOffersByEventId(eventId).stream()
                .map(ticket -> new EventTicketAvailability(
                        ticket.ticketId(),
                        ticket.eventId(),
                        ticket.name(),
                        ticket.priceInCents(),
                        stock.available(ticket.ticketId())))
                .toList();
    }
}

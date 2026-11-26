package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.event.EventTicketAvailability;
import fr.soft_labs.eventbyte.ports.in.GetEventTickets;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** US-11 : GET /events/{eventId}/tickets (route publique). */
@RestController
class EventTicketsController {

    record TicketResponse(String id, String eventId, String name, BigDecimal price, int quantity) {
        static TicketResponse from(EventTicketAvailability ticket) {
            return new TicketResponse(ticket.ticketId(), ticket.eventId(), ticket.name(),
                    ApiPrice.toApi(ticket.priceInCents()), ticket.quantityAvailable());
        }
    }

    private final GetEventTickets getEventTickets;

    EventTicketsController(GetEventTickets getEventTickets) {
        this.getEventTickets = getEventTickets;
    }

    @GetMapping("/events/{eventId}/tickets")
    List<TicketResponse> getTickets(@PathVariable String eventId) {
        return getEventTickets.getForEvent(eventId).stream().map(TicketResponse::from).toList();
    }
}

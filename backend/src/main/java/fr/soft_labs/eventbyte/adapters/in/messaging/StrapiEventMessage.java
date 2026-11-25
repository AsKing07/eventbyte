package fr.soft_labs.eventbyte.adapters.in.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.domain.event.Venue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Message publié par Strapi sur l'exchange "events" (event.created / event.updated).
 * Forme fixée par le README.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record StrapiEventMessage(String id,
                          String title,
                          String description,
                          String cover,
                          Instant startAt,
                          VenueMessage venue,
                          List<TicketMessage> tickets) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VenueMessage(String name, String address, Double latitude, Double longitude) {
    }

    /**
     * price : en EUROS côté Strapi (champ decimal, ex. 30.23), constaté sur les données du CLI,
     * alors que le README annonce des centimes (décision EB-28). Converti en centimes ici.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record TicketMessage(String id, String name, Integer quantity, BigDecimal price) {
    }

    Event toDomain() {
        Objects.requireNonNull(venue, "venue absent du message");
        return new Event(
                id,
                title,
                description,
                cover,
                startAt,
                new Venue(venue.name(), venue.address(),
                        new Coordinates(required(venue.latitude(), "venue.latitude"),
                                        required(venue.longitude(), "venue.longitude"))),
                tickets == null ? List.of() : tickets.stream().map(StrapiEventMessage::toTicket).toList());
    }

    private static Ticket toTicket(TicketMessage t) {
        long priceInCents = required(t.price(), "ticket.price")
                .movePointRight(2)                      // euros -> centimes
                .setScale(0, RoundingMode.HALF_UP)      // au-delà de 2 décimales : arrondi au centime
                .longValueExact();
        return new Ticket(t.id(), t.name(), priceInCents, required(t.quantity(), "ticket.quantity"));
    }

    private static <T> T required(T value, String field) {
        return Objects.requireNonNull(value, field + " absent du message");
    }
}

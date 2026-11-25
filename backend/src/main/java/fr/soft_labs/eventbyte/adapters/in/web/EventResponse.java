package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Forme JSON d'un événement dans le contrat d'API du README
 * (GET /events/discover, /events/:id, /events, /events/search, /events/liked).
 * Attention : l'API expose venue.adresse et venue.coordinates, pas la forme du message Strapi.
 */
public record EventResponse(String id,
                            String title,
                            String cover,
                            long likedCount,
                            VenueResponse venue,
                            boolean isSoldOut,
                            Instant startAt,
                            BigDecimal lowestPrice,
                            String description) {

    public record VenueResponse(String name, String adresse, CoordinatesResponse coordinates) {
    }

    public record CoordinatesResponse(double latitude, double longitude) {
    }

    public static EventResponse from(EventView view) {
        Event event = view.event();
        return new EventResponse(
                event.id(),
                event.title(),
                event.cover(),
                view.likedCount(),
                new VenueResponse(event.venue().name(), event.venue().address(),
                        new CoordinatesResponse(event.venue().coordinates().latitude(),
                                                event.venue().coordinates().longitude())),
                view.soldOut(),
                event.startAt(),
                ApiPrice.toApi(view.lowestPriceInCents()),
                event.description());
    }
}

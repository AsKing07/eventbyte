package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.ports.in.DiscoverEvents;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * US-4 : GET /events/discover?lat&lng (route publique).
 * Le périmètre (rayon, dates, nombre) est fixé par le domaine, décision EB-36.
 */
@RestController
class EventDiscoveryController {

    private final DiscoverEvents discoverEvents;

    EventDiscoveryController(DiscoverEvents discoverEvents) {
        this.discoverEvents = discoverEvents;
    }

    @GetMapping("/events/discover")
    List<EventResponse> discover(@RequestParam double lat, @RequestParam double lng) {
        return discoverEvents.discover(new Coordinates(lat, lng)).stream().map(EventResponse::from).toList();
    }
}

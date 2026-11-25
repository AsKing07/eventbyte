package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.SearchCriteria;
import fr.soft_labs.eventbyte.ports.in.SearchEvents;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** US-7 : GET /events/search?lat&lng&radius&query (route publique). */
@RestController
class EventSearchController {

    private final SearchEvents searchEvents;

    EventSearchController(SearchEvents searchEvents) {
        this.searchEvents = searchEvents;
    }

    @GetMapping("/events/search")
    List<EventResponse> search(@RequestParam double lat,
                               @RequestParam double lng,
                               @RequestParam double radius,
                               @RequestParam(required = false, defaultValue = "") String query) {
        SearchCriteria criteria = new SearchCriteria(new Coordinates(lat, lng), radius, query);
        return searchEvents.search(criteria).stream().map(EventResponse::from).toList();
    }
}

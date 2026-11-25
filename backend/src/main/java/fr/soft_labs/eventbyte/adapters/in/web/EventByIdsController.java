package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.ports.in.GetEventsByIds;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * US-6 : GET /events?ids=id1,id2,id3.
 */
@RestController
class EventByIdsController {

    private final GetEventsByIds getEventsByIds;

    EventByIdsController(GetEventsByIds getEventsByIds) {
        this.getEventsByIds = getEventsByIds;
    }

    @GetMapping("/events")
    List<EventResponse> getByIds(@RequestParam String ids) {
        List<String> eventIds = Arrays.stream(ids.split(","))
                .map(String::strip)
                .filter(id -> !id.isEmpty())
                .distinct()
                .toList();
        return getEventsByIds.getEventsByIds(eventIds).stream().map(EventResponse::from).toList();
    }
}

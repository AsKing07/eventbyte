package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.ports.in.GetEventById;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class EventByIdController {

    private final GetEventById getEventById;

    EventByIdController(GetEventById getEventById) {
        this.getEventById = getEventById;
    }

    @GetMapping("/events/{id}")
    EventResponse getById(@PathVariable String id) {
        return EventResponse.from(getEventById.getEventById(id));
    }
}

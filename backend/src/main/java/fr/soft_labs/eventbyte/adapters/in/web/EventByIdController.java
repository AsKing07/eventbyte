package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.ports.in.GetEventById;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-5 : GET /events/{id} (route publique). Introuvable, supprimé ou sans date : 404 (ApiExceptionHandler).
 * Les chemins fixes (/events/discover, /search, /liked) restent servis par leurs contrôleurs :
 * Spring préfère toujours un chemin littéral à une variable.
 */
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

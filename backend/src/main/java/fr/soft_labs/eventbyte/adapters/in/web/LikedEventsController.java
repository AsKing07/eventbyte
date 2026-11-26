package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.in.ListLikedEvents;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class LikedEventsController {

    private final ListLikedEvents likedEvents;

    LikedEventsController(ListLikedEvents likedEvents) {
        this.likedEvents = likedEvents;
    }

    @GetMapping("/events/liked")
    List<EventResponse> list(@CurrentUser UserId userId) {
        return likedEvents.listLikedEvents(userId).stream()
                .map(EventResponse::from)
                .toList();
    }
}

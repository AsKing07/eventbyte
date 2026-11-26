package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.ports.in.LikeEvent;
import fr.soft_labs.eventbyte.ports.in.UnlikeEvent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LikeEventController {

    private final LikeEvent likeEvent;
    private final UnlikeEvent unlikeEvent;

    LikeEventController(LikeEvent likeEvent, UnlikeEvent unlikeEvent) {
        this.likeEvent = likeEvent;
        this.unlikeEvent = unlikeEvent;
    }

    @PostMapping("/events/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    void like(@CurrentUser UserId userId, @PathVariable("eventId") String eventId) {
        likeEvent.like(userId, eventId);
    }

    @DeleteMapping("/events/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    void unlike(@CurrentUser UserId userId, @PathVariable("eventId") String eventId) {
        unlikeEvent.unlike(userId, eventId);
    }

    @ExceptionHandler(EventNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void eventNotFound() {
    }
}

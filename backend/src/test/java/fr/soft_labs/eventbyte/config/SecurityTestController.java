package fr.soft_labs.eventbyte.config;

import fr.soft_labs.eventbyte.adapters.in.web.CurrentUser;
import fr.soft_labs.eventbyte.domain.UserId;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Faux contrôleur, uniquement pour les tests : il reprend les routes du README
 * pour vérifier les règles de sécurité avant que les vrais contrôleurs existent.
 */
@RestController
class SecurityTestController {

    @GetMapping("/events/discover")
    String discover() { return "public"; }

    @GetMapping("/events/{id}")
    String event(@PathVariable String id) { return id; }

    @GetMapping("/events/{eventId}/tickets")
    String tickets(@PathVariable String eventId) { return "public"; }

    @GetMapping("/events/liked")
    String liked(@CurrentUser UserId userId) { return userId.value(); }

    @DeleteMapping("/events/{eventId}/like")
    String unlike(@PathVariable String eventId, @CurrentUser UserId userId) { return userId.value(); }

    @PostMapping("/bookings")
    String book(@CurrentUser UserId userId) { return userId.value(); }

    @GetMapping("/bookings/user")
    String myBookings(@CurrentUser UserId userId) { return userId.value(); }
}

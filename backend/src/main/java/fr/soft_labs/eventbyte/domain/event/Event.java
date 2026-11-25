package fr.soft_labs.eventbyte.domain.event;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Événement publié par un organisateur dans Strapi.
 *
 * @param startAt date de début ; absente tant que Strapi ne l'envoie pas (décision EB-27)
 */
public record Event(String id,
                    String title,
                    String description,
                    String cover,
                    Instant startAt,
                    Venue venue,
                    List<Ticket> tickets) {

    public Event {
        id = Text.required(id, "L'identifiant de l'événement");
        title = Text.required(title, "Le titre de l'événement");
        description = Objects.requireNonNullElse(description, "");
        cover = Objects.requireNonNullElse(cover, "");
        Objects.requireNonNull(venue, "Le lieu de l'événement est obligatoire");
        tickets = List.copyOf(Objects.requireNonNullElse(tickets, List.of()));

        Set<String> ids = new HashSet<>();
        for (Ticket ticket : tickets) {
            if (!ids.add(ticket.id())) {
                throw new IllegalArgumentException("Ticket en double dans l'événement " + id + " : " + ticket.id());
            }
        }
    }

    public Optional<Instant> startDate() {
        return Optional.ofNullable(startAt);
    }
}

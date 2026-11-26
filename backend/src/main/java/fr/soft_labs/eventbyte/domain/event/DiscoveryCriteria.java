package fr.soft_labs.eventbyte.domain.event;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Critères de GET /events/discover (décision EB-36) : le front n'envoie qu'une position,
 * le périmètre est fixé ici. Événements à venir ou en cours dans un rayon de {@value #RADIUS_KM} km,
 * du plus proche dans le temps au plus lointain, {@value #MAX_RESULTS} au plus.
 * Un événement reste visible {@link #ONGOING_GRACE} après son début : un concert commencé il y a
 * une heure n'a pas encore disparu pour quelqu'un qui arrive.
 *
 * @param startingFrom seuls les événements qui commencent à partir de cet instant sont retenus
 */
public record DiscoveryCriteria(Coordinates center, Instant startingFrom) {

    public static final double RADIUS_KM = 50;
    public static final int MAX_RESULTS = 50;
    public static final Duration ONGOING_GRACE = Duration.ofHours(3);

    /** Critères de découverte autour de cette position, vus à l'instant {@code now}. */
    public static DiscoveryCriteria around(Coordinates center, Instant now) {
        Objects.requireNonNull(now, "L'instant de la recherche est obligatoire");
        return new DiscoveryCriteria(center, now.minus(ONGOING_GRACE));
    }

    public DiscoveryCriteria {
        Objects.requireNonNull(center, "La position est obligatoire");
        Objects.requireNonNull(startingFrom, "La date de début est obligatoire");
    }

    /** Vrai si l'événement fait partie de la découverte du point de vue de la date. */
    public boolean isUpcoming(Event event) {
        return event.startAt() != null && !event.startAt().isBefore(startingFrom);
    }
}

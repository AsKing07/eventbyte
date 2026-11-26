package fr.soft_labs.eventbyte.domain.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Critères de GET /events/discover (décision EB-36) : le front n'envoie qu'une position,
 * le périmètre est fixé ici. Événements à venir dans un rayon de {@value #RADIUS_KM} km,
 * du plus proche dans le temps au plus lointain, {@value #MAX_RESULTS} au plus.
 *
 * @param startingFrom seuls les événements qui commencent à partir de cet instant sont retenus
 */
public record DiscoveryCriteria(Coordinates center, Instant startingFrom) {

    public static final double RADIUS_KM = 50;
    public static final int MAX_RESULTS = 50;

    public DiscoveryCriteria {
        Objects.requireNonNull(center, "La position est obligatoire");
        Objects.requireNonNull(startingFrom, "La date de début est obligatoire");
    }

    /** Vrai si l'événement fait partie de la découverte du point de vue de la date. */
    public boolean isUpcoming(Event event) {
        return event.startAt() != null && !event.startAt().isBefore(startingFrom);
    }
}

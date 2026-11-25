package fr.soft_labs.eventbyte.domain.event;

import java.util.Objects;

/**
 * Critères de GET /events/search : mot-clé dans un rayon autour d'une position.
 *
 * @param radiusKm rayon en kilomètres, strictement positif et au plus {@value #MAX_RADIUS_KM}
 * @param query    mot-clé ; vide = tous les événements du rayon
 */
public record SearchCriteria(Coordinates center, double radiusKm, String query) {

    public static final double MAX_RADIUS_KM = 500;

    public SearchCriteria {
        Objects.requireNonNull(center, "La position de recherche est obligatoire");
        if (!(radiusKm > 0) || radiusKm > MAX_RADIUS_KM) {
            throw new IllegalArgumentException("Rayon invalide : " + radiusKm + " km (entre 0 et " + MAX_RADIUS_KM + ")");
        }
        query = query == null ? "" : query.strip();
    }

    public boolean hasQuery() {
        return !query.isEmpty();
    }
}

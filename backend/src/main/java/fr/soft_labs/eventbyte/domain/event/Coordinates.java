package fr.soft_labs.eventbyte.domain.event;

/**
 * Position GPS d'un lieu, en degrés décimaux.
 */
public record Coordinates(double latitude, double longitude) {

    public Coordinates {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude hors bornes : " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude hors bornes : " + longitude);
        }
    }
}

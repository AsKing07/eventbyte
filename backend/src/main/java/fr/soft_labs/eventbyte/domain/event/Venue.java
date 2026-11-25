package fr.soft_labs.eventbyte.domain.event;

import java.util.Objects;

/**
 * Lieu d'un événement. N'existe pas sans son événement.
 */
public record Venue(String name, String address, Coordinates coordinates) {

    public Venue {
        name = Text.required(name, "Le nom du lieu");
        address = Text.required(address, "L'adresse du lieu");
        Objects.requireNonNull(coordinates, "Les coordonnées du lieu sont obligatoires");
    }
}

package fr.soft_labs.eventbyte.domain;

import java.util.Objects;

/**
 * Identifiant d'un utilisateur : le {@code sub} du JWT Keycloak.
 * Ne vient jamais du corps d'une requête.
 */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "L'identifiant utilisateur est obligatoire");
        if (value.isBlank()) {
            throw new IllegalArgumentException("L'identifiant utilisateur ne peut pas être vide");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

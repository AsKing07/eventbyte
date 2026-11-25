package fr.soft_labs.eventbyte.domain.event;

/**
 * Type de ticket d'un événement (ex. « Early Bird »).
 *
 * @param priceInCents prix unitaire en centimes
 * @param quantity     stock total défini par l'organisateur dans Strapi
 */
public record Ticket(String id, String name, long priceInCents, int quantity) {

    public Ticket {
        id = Text.required(id, "L'identifiant du ticket");
        name = Text.required(name, "Le nom du ticket");
        if (priceInCents < 0) {
            throw new IllegalArgumentException("Prix négatif pour le ticket " + id);
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantité négative pour le ticket " + id);
        }
    }
}

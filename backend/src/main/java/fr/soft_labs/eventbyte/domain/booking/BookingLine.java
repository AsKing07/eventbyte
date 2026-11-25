package fr.soft_labs.eventbyte.domain.booking;

/**
 * Une ligne de réservation : un type de ticket, sa quantité et son prix unitaire
 * figé au moment de la réservation (le prix Strapi peut changer ensuite).
 */
public record BookingLine(String ticketId, String name, int quantity, long unitPriceInCents) {

    public BookingLine {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("L'identifiant du ticket est obligatoire");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantité invalide pour le ticket " + ticketId + " : " + quantity);
        }
        if (unitPriceInCents < 0) {
            throw new IllegalArgumentException("Prix négatif pour le ticket " + ticketId);
        }
    }

    public long subtotalInCents() {
        return unitPriceInCents * quantity;
    }
}

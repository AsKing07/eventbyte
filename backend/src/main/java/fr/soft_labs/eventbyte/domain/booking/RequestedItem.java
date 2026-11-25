package fr.soft_labs.eventbyte.domain.booking;

/** Ce que l'utilisateur demande : un ticket et une quantité. Le prix ne vient jamais du client. */
public record RequestedItem(String ticketId, int quantity) {

    public RequestedItem {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("L'identifiant du ticket est obligatoire");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantité invalide : " + quantity);
        }
    }
}

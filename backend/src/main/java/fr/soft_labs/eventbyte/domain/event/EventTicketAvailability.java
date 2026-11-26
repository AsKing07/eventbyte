package fr.soft_labs.eventbyte.domain.event;

/** Snapshot d'un type de ticket et de son stock actuellement disponible. */
public record EventTicketAvailability(String ticketId,
                                      String eventId,
                                      String name,
                                      long priceInCents,
                                      int quantityAvailable) {

    public EventTicketAvailability {
        ticketId = Text.required(ticketId, "L'identifiant du ticket");
        eventId = Text.required(eventId, "L'identifiant de l'événement");
        name = Text.required(name, "Le nom du ticket");
        if (priceInCents < 0) {
            throw new IllegalArgumentException("Prix négatif pour le ticket " + ticketId);
        }
        if (quantityAvailable < 0) {
            throw new IllegalArgumentException("Quantité disponible négative pour le ticket " + ticketId);
        }
    }
}

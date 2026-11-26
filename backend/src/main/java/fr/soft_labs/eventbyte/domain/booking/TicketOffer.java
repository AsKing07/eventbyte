package fr.soft_labs.eventbyte.domain.booking;

/** Un type de ticket en vente, tel que la réservation le voit (prix et stock total viennent de Strapi). */
public record TicketOffer(String ticketId, String eventId, String name, long priceInCents, int totalQuantity) {
}

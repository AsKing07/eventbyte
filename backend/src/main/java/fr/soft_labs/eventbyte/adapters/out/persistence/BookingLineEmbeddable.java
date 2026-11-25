package fr.soft_labs.eventbyte.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Ligne de la table booking_lines. */
@Embeddable
class BookingLineEmbeddable {

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    protected BookingLineEmbeddable() {
    }

    BookingLineEmbeddable(String ticketId, String name, int quantity, long unitPrice) {
        this.ticketId = ticketId;
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    String getTicketId() { return ticketId; }
    String getName() { return name; }
    int getQuantity() { return quantity; }
    long getUnitPrice() { return unitPrice; }
}

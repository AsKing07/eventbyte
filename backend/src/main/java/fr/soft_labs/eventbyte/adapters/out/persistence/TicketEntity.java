package fr.soft_labs.eventbyte.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Table "tickets" (migration V1). Prix en centimes, quantité = stock total Strapi. */
@Entity
@Table(name = "tickets")
class TicketEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private int quantity;

    protected TicketEntity() {
    }

    TicketEntity(String id, EventEntity event) {
        this.id = id;
        this.event = event;
    }

    String getId() { return id; }
    String getName() { return name; }
    long getPrice() { return price; }
    int getQuantity() { return quantity; }

    void setName(String name) { this.name = name; }
    void setPrice(long price) { this.price = price; }
    void setQuantity(int quantity) { this.quantity = quantity; }
}

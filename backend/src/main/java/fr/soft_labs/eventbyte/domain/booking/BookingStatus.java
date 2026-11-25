package fr.soft_labs.eventbyte.domain.booking;

/** Cycle de vie d'une réservation (contrat du README). */
public enum BookingStatus {
    REQUESTED,
    RESERVED,
    REJECTED,
    AWAITING_PAYMENT,
    EXPIRED,
    CONFIRMED;

    /** Statuts qui immobilisent du stock. */
    public boolean holdsStock() {
        return this == RESERVED || this == AWAITING_PAYMENT || this == CONFIRMED;
    }
}

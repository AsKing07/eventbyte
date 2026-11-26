package fr.soft_labs.eventbyte.domain.booking;

/** Ce que le backend a fait d'un paiement réussi signalé par Stripe (US-17). */
public enum PaymentOutcome {
    /** Réservation en attente de paiement : confirmée. */
    CONFIRMED,
    /** Paiement arrivé après l'expiration, places encore disponibles : reprises et réservation confirmée (EB-33). */
    CONFIRMED_LATE,
    /** Paiement arrivé après l'expiration, plus de places : remboursé, la réservation reste expirée (EB-33). */
    REFUNDED,
    /** Ce paiement a déjà été traité (Stripe renvoie un webhook tant qu'il n'a pas eu de réponse 2xx). */
    ALREADY_PROCESSED,
    /** Aucune réservation ne correspond : rien à faire. */
    UNKNOWN_BOOKING
}

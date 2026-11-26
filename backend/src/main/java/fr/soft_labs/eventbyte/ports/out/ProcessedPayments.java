package fr.soft_labs.eventbyte.ports.out;

/** Paiements Stripe déjà traités : un webhook reçu deux fois n'est traité qu'une fois. */
public interface ProcessedPayments {

    /**
     * Réserve le traitement de ce paiement, de façon atomique.
     *
     * @return vrai si c'est le premier traitement, faux s'il a déjà été pris (doublon ou envoi concurrent)
     */
    boolean claim(String paymentIntentId);

    /** Annule la réservation du traitement après un échec : le prochain envoi de Stripe sera retraité. */
    void release(String paymentIntentId);
}

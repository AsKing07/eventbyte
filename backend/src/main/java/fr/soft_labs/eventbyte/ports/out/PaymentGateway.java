package fr.soft_labs.eventbyte.ports.out;

/** Prestataire de paiement (Stripe). Aucune donnée de carte ne passe par le backend. */
public interface PaymentGateway {

    /**
     * Crée l'intention de paiement de la réservation.
     * Appelée deux fois pour la même réservation, elle ne crée pas un second paiement.
     *
     * @param amountInCents montant total en centimes, frais compris
     * @return le clientSecret que le front passe à Stripe pour saisir la carte
     * @throws fr.soft_labs.eventbyte.domain.booking.PaymentUnavailableException si le prestataire refuse ou ne répond pas
     */
    String createPaymentIntent(String bookingId, long amountInCents);

    /**
     * Rembourse intégralement ce paiement (paiement reçu alors que les places ne sont plus disponibles).
     * Appelée deux fois pour le même paiement, elle ne rembourse qu'une fois.
     */
    void refund(String paymentIntentId);
}

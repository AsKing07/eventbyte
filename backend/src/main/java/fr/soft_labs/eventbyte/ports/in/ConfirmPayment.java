package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.booking.PaymentOutcome;

/** US-17 (EB-23) : Stripe signale qu'un paiement a réussi. */
public interface ConfirmPayment {

    PaymentOutcome paymentSucceeded(String paymentIntentId, String bookingId);
}

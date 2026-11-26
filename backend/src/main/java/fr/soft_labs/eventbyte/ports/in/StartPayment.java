package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.booking.Booking;

/** US-16 (EB-22) : une réservation reserved passe en awaiting-payment avec un clientSecret Stripe. */
public interface StartPayment {

    Booking startPayment(String bookingId);
}

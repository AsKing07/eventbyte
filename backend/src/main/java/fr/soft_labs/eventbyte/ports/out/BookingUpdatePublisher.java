package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.booking.Booking;

/** Publie une réservation après l'enregistrement effectif d'un changement d'état. */
public interface BookingUpdatePublisher {

    void publish(Booking booking);
}

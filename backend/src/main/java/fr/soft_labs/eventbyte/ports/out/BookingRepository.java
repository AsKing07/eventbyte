package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.booking.Booking;

import java.util.Optional;

public interface BookingRepository {

    /** Crée ou met à jour la réservation (statut, clientSecret). */
    void save(Booking booking);

    Optional<Booking> findById(String bookingId);
}

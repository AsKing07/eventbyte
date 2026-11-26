package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    /** Crée ou met à jour la réservation (statut, clientSecret). */
    void save(Booking booking);

    Optional<Booking> findById(String bookingId);

    List<Booking> findUnpaidBookingsPastDeadline(Instant cutoff);

    int expireIfUnpaidAndBookedBefore(String bookingId, Instant cutoff, Instant updatedAt);

    List<Booking> findExpiredBookingsPendingStockReturn();

    void markStockReturned(String bookingId);

    boolean updateIfStatus(Booking booking, BookingStatus expectedStatus);
}

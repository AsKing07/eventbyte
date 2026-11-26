package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;

import java.util.List;

/** US-15 (EB-21) : un utilisateur connecté consulte ses réservations, et seulement les siennes. */
public interface ViewBookings {

    /** @throws fr.soft_labs.eventbyte.domain.booking.BookingNotFoundException si absente ou à un autre utilisateur */
    Booking getBooking(UserId userId, String bookingId);

    /** Ses réservations, la plus récente d'abord. */
    List<Booking> getUserBookings(UserId userId);
}

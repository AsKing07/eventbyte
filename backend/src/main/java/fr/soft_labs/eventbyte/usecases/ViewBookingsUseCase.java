package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingNotFoundException;
import fr.soft_labs.eventbyte.ports.in.ViewBookings;
import fr.soft_labs.eventbyte.ports.out.BookingRepository;

import java.util.List;
import java.util.Objects;

/** L'utilisateur vient toujours du JWT : il ne peut lire que ses propres réservations. */
public class ViewBookingsUseCase implements ViewBookings {

    private final BookingRepository bookings;

    public ViewBookingsUseCase(BookingRepository bookings) {
        this.bookings = Objects.requireNonNull(bookings);
    }

    @Override
    public Booking getBooking(UserId userId, String bookingId) {
        Objects.requireNonNull(userId, "L'utilisateur est obligatoire");
        return bookings.findById(bookingId)
                .filter(booking -> booking.belongsTo(userId))   // celle d'un autre : 404, comme une absente
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    @Override
    public List<Booking> getUserBookings(UserId userId) {
        Objects.requireNonNull(userId, "L'utilisateur est obligatoire");
        return bookings.findByUser(userId);
    }
}

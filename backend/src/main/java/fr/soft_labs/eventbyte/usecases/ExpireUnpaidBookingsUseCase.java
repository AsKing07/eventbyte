package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.ports.in.ExpireUnpaidBookings;
import fr.soft_labs.eventbyte.ports.out.BookingRepository;
import fr.soft_labs.eventbyte.ports.out.TicketStock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Expiration backend des réservations impayées, indépendante du compte à rebours du frontend. */
public class ExpireUnpaidBookingsUseCase implements ExpireUnpaidBookings {

    private final BookingRepository bookings;
    private final TicketStock stock;
    private final Clock clock;
    private final Duration expirationDelay;

    public ExpireUnpaidBookingsUseCase(BookingRepository bookings, TicketStock stock, Clock clock,
                                      Duration expirationDelay) {
        this.bookings = Objects.requireNonNull(bookings);
        this.stock = Objects.requireNonNull(stock);
        this.clock = Objects.requireNonNull(clock);
        this.expirationDelay = Objects.requireNonNull(expirationDelay);
        if (expirationDelay.isNegative() || expirationDelay.isZero()) {
            throw new IllegalArgumentException("Le délai d'expiration doit être positif");
        }
    }

    @Override
    public int expireOverdueBookings() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(expirationDelay);
        int expiredCount = 0;

        for (Booking booking : bookings.findUnpaidBookingsPastDeadline(cutoff)) {
            if (booking.expireIfOverdue(clock, expirationDelay).isPresent()
                    && bookings.expireIfUnpaidAndBookedBefore(booking.id(), cutoff, now) == 1) {
                expiredCount++;
            }
        }

        for (Booking booking : bookings.findExpiredBookingsPendingStockReturn()) {
            stock.giveBackOnce(booking.id(), booking.lines());
            bookings.markStockReturned(booking.id());
        }
        return expiredCount;
    }
}

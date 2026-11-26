package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Représentation publique d'une réservation, partagée par les réponses et le flux SSE. */
record BookingResponse(String id,
                       String eventId,
                       Instant bookedAt,
                       List<BookingLineResponse> lines,
                       String status,
                       BigDecimal total,
                       BigDecimal fees,
                       String clientSecret) {

    record BookingLineResponse(String id, String name, int quantity, BigDecimal price) {
        static BookingLineResponse from(BookingLine line) {
            return new BookingLineResponse(line.ticketId(), line.name(), line.quantity(),
                    ApiPrice.toApi(line.unitPriceInCents()));
        }
    }

    static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.id(),
                booking.eventId(),
                booking.bookedAt(),
                booking.lines().stream().map(BookingLineResponse::from).toList(),
                apiStatus(booking.status()),
                ApiPrice.toApi(booking.totalInCents()),
                ApiPrice.toApi(booking.feesInCents()),
                booking.clientSecret());
    }

    private static String apiStatus(BookingStatus status) {
        return status.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}

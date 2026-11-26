package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.RequestedItem;
import fr.soft_labs.eventbyte.ports.in.RequestBooking;
import fr.soft_labs.eventbyte.ports.in.ViewBookings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * US-12 : POST /bookings (authentifié).
 * Corps : { items: [{ id: ticketId, quantity }] } ; réponse : { id }.
 * L'utilisateur vient du JWT ; le prix vient de la base, jamais du corps de la requête.
 */
@RestController
class BookingController {

    record BookingRequest(@NotEmpty List<@Valid Item> items) {
        record Item(@NotBlank String id, @Positive int quantity) {
        }
    }

    record BookingCreated(String id) {
    }

    private final RequestBooking requestBooking;
    private final ViewBookings viewBookings;

    BookingController(RequestBooking requestBooking, ViewBookings viewBookings) {
        this.requestBooking = requestBooking;
        this.viewBookings = viewBookings;
    }

    /** US-15 : ses réservations, la plus récente d'abord. Spring préfère ce chemin littéral à /bookings/{id}. */
    @GetMapping("/bookings/user")
    List<BookingResponse> userBookings(@CurrentUser UserId userId) {
        return viewBookings.getUserBookings(userId).stream().map(BookingResponse::from).toList();
    }

    /** US-15 : 404 si absente ou à un autre utilisateur. */
    @GetMapping("/bookings/{bookingId}")
    BookingResponse booking(@CurrentUser UserId userId, @PathVariable String bookingId) {
        return BookingResponse.from(viewBookings.getBooking(userId, bookingId));
    }

    @PostMapping("/bookings")
    BookingCreated book(@CurrentUser UserId userId, @Valid @RequestBody BookingRequest request) {
        List<RequestedItem> items = request.items().stream()
                .map(item -> new RequestedItem(item.id(), item.quantity()))
                .toList();
        return new BookingCreated(requestBooking.book(userId, items).id());
    }
}

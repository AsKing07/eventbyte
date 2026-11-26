package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.in.SubscribeToBookingUpdates;
import fr.soft_labs.eventbyte.ports.in.SubscribeToBookingUpdates.Subscription;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Adaptateur SSE : l'identité vient exclusivement du JWT validé par Spring Security. */
@RestController
class BookingStreamController {

    private final SubscribeToBookingUpdates bookingUpdates;

    BookingStreamController(SubscribeToBookingUpdates bookingUpdates) {
        this.bookingUpdates = bookingUpdates;
    }

    @GetMapping(path = "/bookings/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@CurrentUser UserId userId) {
        SseEmitter emitter = new SseEmitter();
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();
        AtomicBoolean cleanupRequested = new AtomicBoolean();
        Runnable unsubscribe = () -> {
            cleanupRequested.set(true);
            Subscription subscription = subscriptionReference.get();
            if (subscription != null) {
                subscription.close();
            }
        };

        Subscription subscription = bookingUpdates.subscribe(userId, booking -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("booking_updated")
                        .data(BookingResponse.from(booking)));
            } catch (IOException | IllegalStateException e) {
                emitter.completeWithError(e);
                unsubscribe.run();
            }
        });
        subscriptionReference.set(subscription);
        if (cleanupRequested.get()) {
            subscription.close();
        }

        emitter.onCompletion(unsubscribe);
        emitter.onTimeout(unsubscribe);
        emitter.onError(error -> unsubscribe.run());
        return emitter;
    }
}

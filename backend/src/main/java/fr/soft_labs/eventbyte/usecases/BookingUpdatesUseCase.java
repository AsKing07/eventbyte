package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.ports.in.SubscribeToBookingUpdates;
import fr.soft_labs.eventbyte.ports.out.BookingUpdatePublisher;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/** Diffuse les changements uniquement aux abonnés appartenant à l'utilisateur de la réservation. */
public class BookingUpdatesUseCase implements SubscribeToBookingUpdates, BookingUpdatePublisher {

    private final ConcurrentHashMap<UserId, CopyOnWriteArraySet<Consumer<Booking>>> listeners =
            new ConcurrentHashMap<>();

    @Override
    public Subscription subscribe(UserId userId, Consumer<Booking> listener) {
        Objects.requireNonNull(userId, "L'utilisateur est obligatoire");
        Objects.requireNonNull(listener, "Le listener est obligatoire");
        listeners.compute(userId, (key, current) -> {
            CopyOnWriteArraySet<Consumer<Booking>> userListeners = current == null
                    ? new CopyOnWriteArraySet<>()
                    : current;
            userListeners.add(listener);
            return userListeners;
        });
        return () -> listeners.computeIfPresent(userId, (key, current) -> {
            current.remove(listener);
            return current.isEmpty() ? null : current;
        });
    }

    @Override
    public void publish(Booking booking) {
        Objects.requireNonNull(booking, "La réservation est obligatoire");
        CopyOnWriteArraySet<Consumer<Booking>> userListeners = listeners.get(booking.userId());
        if (userListeners != null) {
            userListeners.forEach(listener -> listener.accept(booking));
        }
    }
}

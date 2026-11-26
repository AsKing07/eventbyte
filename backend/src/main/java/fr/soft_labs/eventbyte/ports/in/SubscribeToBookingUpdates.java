package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;

import java.util.function.Consumer;

/** Ouvre une souscription aux changements de réservation d'un utilisateur. */
public interface SubscribeToBookingUpdates {

    Subscription subscribe(UserId userId, Consumer<Booking> listener);

    interface Subscription {
        void close();
    }
}

package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.RequestedItem;

import java.util.List;

/** US-12 : réserver une ou plusieurs places. */
public interface RequestBooking {

    /**
     * Crée la réservation (requested), puis la passe en reserved si le stock suffit, rejected sinon.
     *
     * @throws IllegalArgumentException ticket inconnu, tickets de plusieurs événements, quantité invalide
     */
    Booking book(UserId userId, List<RequestedItem> items);
}

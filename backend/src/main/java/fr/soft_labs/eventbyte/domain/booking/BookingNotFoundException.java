package fr.soft_labs.eventbyte.domain.booking;

/**
 * Réservation absente, ou appartenant à quelqu'un d'autre : même erreur dans les deux cas,
 * pour ne pas révéler qu'un identifiant existe.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String bookingId) {
        super("Réservation introuvable : " + bookingId);
    }
}

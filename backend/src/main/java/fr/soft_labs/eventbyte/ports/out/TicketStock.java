package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.booking.BookingLine;

import java.util.List;

/**
 * Stock disponible des tickets. Implémentation attendue : atomique (Redis),
 * deux réservations simultanées du dernier ticket ne peuvent pas réussir toutes les deux (US-13).
 */
public interface TicketStock {

    /**
     * Retire les quantités de toutes les lignes, ou rien du tout si une seule manque de stock.
     *
     * @return vrai si le stock a été pris
     */
    boolean tryTake(List<BookingLine> lines);

    /** Remet en vente les quantités (réservation expirée, échec technique après prise du stock). */
    void giveBack(List<BookingLine> lines);
}

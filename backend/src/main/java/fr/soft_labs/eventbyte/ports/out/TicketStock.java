package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.booking.BookingLine;

import java.util.Collection;
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

    /** Nombre de places pouvant encore être réservées pour ce ticket. */
    int available(String ticketId);

    /** Remet en vente les quantités (réservation expirée, échec technique après prise du stock). */
    void giveBack(List<BookingLine> lines);

    /**
     * Le stock total de ces tickets a pu changer dans Strapi (EB-30) : le stock disponible
     * sera recalculé depuis la base à la prochaine réservation.
     */
    void refresh(Collection<String> ticketIds);
}

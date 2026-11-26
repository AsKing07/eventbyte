package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.booking.TicketOffer;

import java.util.Collection;
import java.util.List;

public interface TicketCatalog {

    /** Tickets en vente parmi ces ids (événements non supprimés) ; les ids inconnus sont absents du résultat. */
    List<TicketOffer> findOffers(Collection<String> ticketIds);
}

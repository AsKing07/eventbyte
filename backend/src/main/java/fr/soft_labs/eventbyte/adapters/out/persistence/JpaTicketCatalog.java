package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.domain.booking.TicketOffer;
import fr.soft_labs.eventbyte.ports.out.TicketCatalog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Component
class JpaTicketCatalog implements TicketCatalog {

    private final TicketJpaRepository jpa;

    JpaTicketCatalog(TicketJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketOffer> findOffers(Collection<String> ticketIds) {
        return ticketIds.isEmpty() ? List.of() : jpa.findOffers(ticketIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketOffer> findOffersByEventId(String eventId) {
        return jpa.findOffersByEventId(eventId);
    }
}

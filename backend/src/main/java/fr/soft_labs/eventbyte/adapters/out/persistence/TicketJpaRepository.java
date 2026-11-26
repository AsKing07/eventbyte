package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.domain.booking.TicketOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface TicketJpaRepository extends JpaRepository<TicketEntity, String> {

    /** Tickets en vente : leur événement n'est pas supprimé. */
    @Query("""
            select new fr.soft_labs.eventbyte.domain.booking.TicketOffer(t.id, e.id, t.name, t.price, t.quantity)
            from TicketEntity t join t.event e
            where t.id in :ids and e.deletedAt is null
            """)
    List<TicketOffer> findOffers(@Param("ids") Collection<String> ids);

    @Query("""
            select new fr.soft_labs.eventbyte.domain.booking.TicketOffer(t.id, e.id, t.name, t.price, t.quantity)
            from TicketEntity t join t.event e
            where e.id = :eventId and e.deletedAt is null
            order by t.id
            """)
    List<TicketOffer> findOffersByEventId(@Param("eventId") String eventId);
}

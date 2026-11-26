package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.booking.TicketOffer;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.domain.event.EventTicketAvailability;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.out.TicketCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetEventTicketsUseCaseTest {

    /** Faux catalogue en mémoire : les offres connues, tous événements confondus. */
    static class InMemoryTicketCatalog implements TicketCatalog {
        final List<TicketOffer> offers = new ArrayList<>();

        @Override
        public List<TicketOffer> findOffers(Collection<String> ticketIds) {
            return offers.stream().filter(offer -> ticketIds.contains(offer.ticketId())).toList();
        }

        @Override
        public List<TicketOffer> findOffersByEventId(String eventId) {
            return offers.stream().filter(offer -> offer.eventId().equals(eventId)).toList();
        }
    }

    InMemoryEventRepository events;
    InMemoryTicketCatalog catalog;
    RequestBookingUseCaseTest.FakeStock stock;
    GetEventTicketsUseCase useCase;

    @BeforeEach
    void setUp() {
        events = new InMemoryEventRepository();
        catalog = new InMemoryTicketCatalog();
        stock = new RequestBookingUseCaseTest.FakeStock();
        useCase = new GetEventTicketsUseCase(events, catalog, stock);
    }

    void evenement(String id) {
        events.save(new Event(id, "Concert", "", "", Instant.parse("2026-12-31T21:00:00Z"),
                new Venue("Le Rex", "Paris", new Coordinates(48.87, 2.34)), List.of()));
    }

    @Test
    void renvoie_les_tickets_avec_le_stock_disponible_et_le_prix_du_catalogue() {
        evenement("evt_1");
        catalog.offers.add(new TicketOffer("tkt_1", "evt_1", "Standard", 3023, 10));   // 10 au total
        catalog.offers.add(new TicketOffer("tkt_autre", "evt_2", "VIP", 9000, 5));     // autre événement
        stock.available.put("tkt_1", 4);                                               // 6 déjà réservées

        assertThat(useCase.getForEvent("evt_1")).containsExactly(
                new EventTicketAvailability("tkt_1", "evt_1", "Standard", 3023, 4));
    }

    @Test
    void un_ticket_epuise_a_une_quantite_de_zero() {
        evenement("evt_1");
        catalog.offers.add(new TicketOffer("tkt_1", "evt_1", "Standard", 3023, 10));
        stock.available.put("tkt_1", 0);

        assertThat(useCase.getForEvent("evt_1")).extracting(EventTicketAvailability::quantityAvailable)
                .containsExactly(0);
    }

    @Test
    void un_evenement_sans_ticket_renvoie_une_liste_vide() {
        evenement("evt_1");

        assertThat(useCase.getForEvent("evt_1")).isEmpty();
    }

    @Test
    void refuse_un_evenement_inconnu_ou_supprime() {
        evenement("evt_supprime");
        events.markDeleted("evt_supprime", Instant.parse("2026-11-26T00:00:00Z"));

        assertThatThrownBy(() -> useCase.getForEvent("evt_inconnu")).isInstanceOf(EventNotFoundException.class);
        assertThatThrownBy(() -> useCase.getForEvent("evt_supprime")).isInstanceOf(EventNotFoundException.class);
    }
}

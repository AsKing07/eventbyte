package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.domain.event.EventTicketAvailability;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.booking.TicketOffer;
import fr.soft_labs.eventbyte.ports.out.EventRepository;
import fr.soft_labs.eventbyte.ports.out.TicketCatalog;
import fr.soft_labs.eventbyte.ports.out.TicketStock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetEventTicketsUseCaseTest {

    @Test
    void renvoie_les_tickets_avec_le_stock_disponible_et_le_prix_du_catalogue() {
        EventRepository events = eventRepository(true);
        TicketCatalog catalog = new TicketCatalog() {
            @Override
            public List<TicketOffer> findOffers(Collection<String> ticketIds) {
                return List.of();
            }

            @Override
            public List<TicketOffer> findOffersByEventId(String eventId) {
                return List.of(new TicketOffer("tkt_1", eventId, "Standard", 3023, 10));
            }
        };
        TicketStock stock = stockReturning(4);
        GetEventTicketsUseCase useCase = new GetEventTicketsUseCase(events, catalog, stock);

        assertThat(useCase.getForEvent("evt_1")).containsExactly(
                new EventTicketAvailability("tkt_1", "evt_1", "Standard", 3023, 4));
    }

    @Test
    void refuse_un_evenement_inconnu_ou_supprime() {
        TicketCatalog catalog = new TicketCatalog() {
            @Override
            public List<TicketOffer> findOffers(Collection<String> ticketIds) {
                return List.of();
            }

            @Override
            public List<TicketOffer> findOffersByEventId(String eventId) {
                return List.of();
            }
        };
        GetEventTicketsUseCase useCase = new GetEventTicketsUseCase(
                eventRepository(false), catalog, stockReturning(0));

        assertThatThrownBy(() -> useCase.getForEvent("evt_hidden"))
                .isInstanceOf(EventNotFoundException.class);
    }

    private static TicketStock stockReturning(int available) {
        return new TicketStock() {
            @Override
            public boolean tryTake(List<BookingLine> lines) {
                return false;
            }

            @Override
            public int available(String ticketId) {
                return available;
            }

            @Override
            public void giveBack(List<BookingLine> lines) {
            }
        };
    }

    private static EventRepository eventRepository(boolean visible) {
        return new EventRepository() {
            @Override
            public void save(Event event) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isDeleted(String eventId) {
                return !visible;
            }

            @Override
            public boolean isVisible(String eventId) {
                return visible;
            }

            @Override
            public boolean markDeleted(String eventId, Instant deletedAt) {
                throw new UnsupportedOperationException();
            }
        };
    }
}

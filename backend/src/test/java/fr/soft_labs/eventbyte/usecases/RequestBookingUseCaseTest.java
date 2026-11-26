package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import fr.soft_labs.eventbyte.domain.booking.RequestedItem;
import fr.soft_labs.eventbyte.domain.booking.TicketOffer;
import fr.soft_labs.eventbyte.ports.out.BookingRepository;
import fr.soft_labs.eventbyte.ports.out.TicketCatalog;
import fr.soft_labs.eventbyte.ports.out.TicketStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestBookingUseCaseTest {

    static final UserId ALICE = new UserId("alice");
    static final Instant NOW = Instant.parse("2026-11-25T18:00:00Z");

    /** Faux catalogue : les offres connues. */
    final Map<String, TicketOffer> offers = new HashMap<>();
    final TicketCatalog catalog = new TicketCatalog() {
        @Override
        public List<TicketOffer> findOffers(Collection<String> ids) {
            return ids.stream().filter(offers::containsKey).map(offers::get).toList();
        }

        @Override
        public List<TicketOffer> findOffersByEventId(String eventId) {
            return List.of();
        }
    };

    /** Faux stock : tout ou rien, comme le script Redis. */
    static class FakeStock implements TicketStock {
        final Map<String, Integer> available = new HashMap<>();

        @Override
        public boolean tryTake(List<BookingLine> lines) {
            boolean enough = lines.stream().allMatch(l -> available.getOrDefault(l.ticketId(), 0) >= l.quantity());
            if (enough) {
                lines.forEach(l -> available.merge(l.ticketId(), -l.quantity(), Integer::sum));
            }
            return enough;
        }

        @Override
        public int available(String ticketId) {
            return available.getOrDefault(ticketId, 0);
        }

        @Override
        public void giveBack(List<BookingLine> lines) {
            lines.forEach(l -> available.merge(l.ticketId(), l.quantity(), Integer::sum));
        }

        @Override
        public void refresh(Collection<String> ticketIds) {
        }
    }

    /** Faux repository : garde chaque état enregistré pour vérifier la séquence de statuts. */
    static class RecordingRepository implements BookingRepository {
        final List<BookingStatus> savedStatuses = new ArrayList<>();
        final Map<String, Booking> bookings = new HashMap<>();
        boolean failOnReserved;

        @Override
        public void save(Booking booking) {
            if (failOnReserved && booking.status() == BookingStatus.RESERVED) {
                throw new IllegalStateException("base indisponible");
            }
            savedStatuses.add(booking.status());
            bookings.put(booking.id(), booking);
        }

        @Override
        public Optional<Booking> findById(String bookingId) {
            return Optional.ofNullable(bookings.get(bookingId));
        }
    }

    FakeStock stock;
    RecordingRepository repository;
    RequestBookingUseCase useCase;

    @BeforeEach
    void setUp() {
        offers.put("tkt_std", new TicketOffer("tkt_std", "evt_1", "Standard", 2200, 80));
        offers.put("tkt_vip", new TicketOffer("tkt_vip", "evt_1", "VIP", 3550, 20));
        offers.put("tkt_lyon", new TicketOffer("tkt_lyon", "evt_lyon", "Standard", 2800, 500));
        stock = new FakeStock();
        stock.available.putAll(Map.of("tkt_std", 80, "tkt_vip", 1, "tkt_lyon", 500));
        repository = new RecordingRepository();
        useCase = new RequestBookingUseCase(catalog, stock, repository, () -> "bk_1", Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void stock_suffisant_la_reservation_passe_de_requested_a_reserved() {
        Booking booking = useCase.book(ALICE, List.of(new RequestedItem("tkt_std", 2)));

        assertThat(booking.status()).isEqualTo(BookingStatus.RESERVED);
        assertThat(repository.savedStatuses).containsExactly(BookingStatus.REQUESTED, BookingStatus.RESERVED);
        assertThat(stock.available).containsEntry("tkt_std", 78);
    }

    @Test
    void les_prix_et_noms_viennent_du_catalogue() {
        Booking booking = useCase.book(ALICE, List.of(new RequestedItem("tkt_std", 2), new RequestedItem("tkt_vip", 1)));

        assertThat(booking.id()).isEqualTo("bk_1");
        assertThat(booking.userId()).isEqualTo(ALICE);
        assertThat(booking.eventId()).isEqualTo("evt_1");
        assertThat(booking.bookedAt()).isEqualTo(NOW);
        assertThat(booking.lines()).containsExactly(
                new BookingLine("tkt_std", "Standard", 2, 2200),
                new BookingLine("tkt_vip", "VIP", 1, 3550));
    }

    @Test
    void stock_insuffisant_la_reservation_est_rejected_et_rien_n_est_pris() {
        Booking booking = useCase.book(ALICE, List.of(new RequestedItem("tkt_std", 2), new RequestedItem("tkt_vip", 2)));

        assertThat(booking.status()).isEqualTo(BookingStatus.REJECTED);
        assertThat(repository.savedStatuses).containsExactly(BookingStatus.REQUESTED, BookingStatus.REJECTED);
        assertThat(stock.available).containsEntry("tkt_std", 80).containsEntry("tkt_vip", 1);   // tout ou rien
    }

    @Test
    void si_l_enregistrement_echoue_apres_la_prise_du_stock_les_places_sont_rendues() {
        repository.failOnReserved = true;

        assertThatThrownBy(() -> useCase.book(ALICE, List.of(new RequestedItem("tkt_vip", 1))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(stock.available).containsEntry("tkt_vip", 1);
    }

    @Test
    void refuse_un_ticket_inconnu() {
        assertThatThrownBy(() -> useCase.book(ALICE, List.of(new RequestedItem("tkt_inconnu", 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.savedStatuses).isEmpty();
    }

    @Test
    void refuse_des_tickets_de_plusieurs_evenements() {
        assertThatThrownBy(() -> useCase.book(ALICE, List.of(new RequestedItem("tkt_std", 1), new RequestedItem("tkt_lyon", 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_une_demande_vide_ou_un_ticket_demande_deux_fois() {
        assertThatThrownBy(() -> useCase.book(ALICE, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.book(ALICE, List.of(new RequestedItem("tkt_std", 1), new RequestedItem("tkt_std", 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

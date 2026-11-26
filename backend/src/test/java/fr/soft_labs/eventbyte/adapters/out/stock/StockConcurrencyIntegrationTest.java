package fr.soft_labs.eventbyte.adapters.out.stock;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import fr.soft_labs.eventbyte.domain.booking.RequestedItem;
import fr.soft_labs.eventbyte.ports.in.RequestBooking;
import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * US-13 (EB-19) : ne jamais survendre, prouvé avec de vraies requêtes simultanées
 * sur Postgres + Redis (Testcontainers). Docker requis, sinon ignoré.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class StockConcurrencyIntegrationTest {

    @Autowired
    RequestBooking requestBooking;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    RabbitTemplate rabbit;

    /** Crée un événement et ses tickets (quantités données), ids uniques pour isoler le stock Redis. */
    List<String> eventWithTickets(int... quantities) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String eventId = "evt_" + suffix;
        jdbc.update("""
                insert into events (id, title, description, cover, start_at, venue_name, venue_address,
                                    venue_latitude, venue_longitude, updated_at)
                values (?, 'Masterclass', 'd', 'https://img', now() + interval '10 days', 'New Morning', 'Paris',
                        48.87, 2.35, now())""", eventId);
        List<String> tickets = new ArrayList<>();
        for (int i = 0; i < quantities.length; i++) {
            String ticketId = "tkt_" + suffix + "_" + i;
            jdbc.update("insert into tickets (id, event_id, name, price, quantity) values (?, ?, ?, 1500, ?)",
                    ticketId, eventId, "Place " + i, quantities[i]);
            tickets.add(ticketId);
        }
        return tickets;
    }

    /** Lance toutes les demandes au même instant (barrière de départ) et renvoie les statuts obtenus. */
    List<BookingStatus> concurrently(int requests, List<RequestedItem> items) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<BookingStatus>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                UserId user = new UserId("user-" + i);
                Callable<BookingStatus> call = () -> {
                    start.await();
                    return requestBooking.book(user, items).status();
                };
                results.add(pool.submit(call));
            }
            start.countDown();
            List<BookingStatus> statuses = new ArrayList<>();
            for (Future<BookingStatus> result : results) {
                statuses.add(result.get());
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    long count(List<BookingStatus> statuses, BookingStatus status) {
        return statuses.stream().filter(status::equals).count();
    }

    @Test
    void vingt_demandes_simultanees_du_dernier_ticket_une_seule_reussit() throws Exception {
        String lastTicket = eventWithTickets(1).get(0);

        List<BookingStatus> statuses = concurrently(20, List.of(new RequestedItem(lastTicket, 1)));

        assertThat(count(statuses, BookingStatus.AWAITING_PAYMENT)).isEqualTo(1);
        assertThat(count(statuses, BookingStatus.REJECTED)).isEqualTo(19);
        assertThat(redis.opsForValue().get("stock:ticket:" + lastTicket)).isEqualTo("0");
    }

    @Test
    void plusieurs_tickets_en_concurrence_tout_ou_rien() throws Exception {
        List<String> tickets = eventWithTickets(3, 3);
        List<RequestedItem> oneOfEach = List.of(new RequestedItem(tickets.get(0), 1), new RequestedItem(tickets.get(1), 1));

        List<BookingStatus> statuses = concurrently(10, oneOfEach);

        assertThat(count(statuses, BookingStatus.AWAITING_PAYMENT)).isEqualTo(3);
        // Jamais une ligne prise sans l'autre : les deux stocks tombent à 0 ensemble
        assertThat(redis.opsForValue().get("stock:ticket:" + tickets.get(0))).isEqualTo("0");
        assertThat(redis.opsForValue().get("stock:ticket:" + tickets.get(1))).isEqualTo("0");
    }

    // ---------- EB-30 : l'organisateur change la quantité dans Strapi ----------

    void publish(String routingKey, String eventId, String ticketId, int quantity) {
        String json = """
                { "id": "%s", "title": "Concert", "description": "d", "cover": "https://img",
                  "startAt": "2026-12-31T21:00:00.000Z",
                  "venue": { "name": "Le Rex", "address": "Paris", "latitude": 48.87, "longitude": 2.34 },
                  "tickets": [{ "id": "%s", "name": "Standard", "quantity": %d, "price": 25.00 }] }
                """.formatted(eventId, ticketId, quantity);
        rabbit.send("events", routingKey, new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties()));
    }

    BookingStatus book(String ticketId, int quantity) {
        return requestBooking.book(new UserId("alice"), List.of(new RequestedItem(ticketId, quantity))).status();
    }

    @Test
    void baisser_la_quantite_dans_strapi_reduit_le_stock_disponible() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String eventId = "evt_" + suffix, ticketId = "tkt_" + suffix, key = "stock:ticket:" + ticketId;

        publish("event.created", eventId, ticketId, 10);
        await().atMost(Duration.ofSeconds(10)).until(() ->
                jdbc.queryForList("select 1 from tickets where id = ?", ticketId).size() == 1);
        assertThat(book(ticketId, 3)).isEqualTo(BookingStatus.AWAITING_PAYMENT);   // stock Redis : 7

        publish("event.updated", eventId, ticketId, 5);                    // 5 au total, 3 déjà réservées
        await().atMost(Duration.ofSeconds(10)).until(() ->
                jdbc.queryForObject("select quantity from tickets where id = ?", Integer.class, ticketId) == 5
                && !Boolean.TRUE.equals(redis.hasKey(key)));

        assertThat(book(ticketId, 3)).isEqualTo(BookingStatus.REJECTED);   // il n'en reste que 2, pas 7
        assertThat(book(ticketId, 2)).isEqualTo(BookingStatus.AWAITING_PAYMENT);
        assertThat(redis.opsForValue().get(key)).isEqualTo("0");
    }

    @Test
    void baisser_la_quantite_sous_le_deja_reserve_donne_zero_sans_annuler() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String eventId = "evt_" + suffix, ticketId = "tkt_" + suffix, key = "stock:ticket:" + ticketId;

        publish("event.created", eventId, ticketId, 10);
        await().atMost(Duration.ofSeconds(10)).until(() ->
                jdbc.queryForList("select 1 from tickets where id = ?", ticketId).size() == 1);
        assertThat(book(ticketId, 4)).isEqualTo(BookingStatus.AWAITING_PAYMENT);

        publish("event.updated", eventId, ticketId, 2);                    // moins que les 4 déjà réservées
        await().atMost(Duration.ofSeconds(10)).until(() ->
                jdbc.queryForObject("select quantity from tickets where id = ?", Integer.class, ticketId) == 2
                && !Boolean.TRUE.equals(redis.hasKey(key)));

        assertThat(book(ticketId, 1)).isEqualTo(BookingStatus.REJECTED);
        assertThat(jdbc.queryForObject("select count(*) from bookings b join booking_lines l on l.booking_id = b.id "
                + "where l.ticket_id = ? and b.status = 'AWAITING_PAYMENT'", Integer.class, ticketId)).isEqualTo(1);   // non annulée
    }
}

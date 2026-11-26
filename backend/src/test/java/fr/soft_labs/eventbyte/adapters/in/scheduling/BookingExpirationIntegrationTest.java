package fr.soft_labs.eventbyte.adapters.in.scheduling;

import fr.soft_labs.eventbyte.ports.in.ExpireUnpaidBookings;
import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-14 sur une vraie base et un vrai Redis : les requêtes JPQL conditionnelles, la colonne stock_returned (V5)
 * et le script Lua de restitution. On vérifie l'état final, pas le nombre renvoyé : le scheduler tourne aussi
 * en arrière-plan et peut passer avant l'appel du test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class BookingExpirationIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    ExpireUnpaidBookings expireUnpaidBookings;

    String ticketId;

    @BeforeEach
    void createEvent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String eventId = "evt_" + suffix;
        ticketId = "tkt_" + suffix;
        jdbc.update("""
                insert into events (id, title, description, cover, start_at, venue_name, venue_address,
                                    venue_latitude, venue_longitude)
                values (?, 'Concert', 'd', 'https://img', now() + interval '10 days', 'Le Rex', 'Paris', 48.87, 2.34)
                """, eventId);
        jdbc.update("insert into tickets (id, event_id, name, price, quantity) values (?, ?, 'Standard', 2500, 10)",
                ticketId, eventId);
    }

    String book(int quantity) throws Exception {
        String json = mvc.perform(post("/bookings").with(jwt().jwt(j -> j.subject("alice")))
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content("{ \"items\": [ { \"id\": \"" + ticketId + "\", \"quantity\": " + quantity + " } ] }"))
                         .andExpect(status().isOk())
                         .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    void bookedMinutesAgo(String bookingId, int minutes) {
        jdbc.update("update bookings set booked_at = now() - make_interval(mins => ?) where id = ?", minutes, bookingId);
    }

    String statusOf(String bookingId) {
        return jdbc.queryForObject("select status from bookings where id = ?", String.class, bookingId);
    }

    String stock() {
        return redis.opsForValue().get("stock:ticket:" + ticketId);
    }

    @Test
    void une_reservation_impayee_depuis_plus_de_10_min_expire_et_rend_ses_places() throws Exception {
        String bookingId = book(3);
        assertThat(stock()).isEqualTo("7");
        bookedMinutesAgo(bookingId, 11);

        expireUnpaidBookings.expireOverdueBookings();

        assertThat(statusOf(bookingId)).isEqualTo("EXPIRED");
        assertThat(jdbc.queryForObject("select stock_returned from bookings where id = ?", Boolean.class, bookingId)).isTrue();
        assertThat(stock()).isEqualTo("10");
    }

    @Test
    void un_second_passage_ne_rend_pas_les_places_deux_fois() throws Exception {
        String bookingId = book(3);
        bookedMinutesAgo(bookingId, 11);

        expireUnpaidBookings.expireOverdueBookings();
        expireUnpaidBookings.expireOverdueBookings();

        assertThat(stock()).isEqualTo("10");
    }

    @Test
    void une_reservation_recente_ou_confirmee_n_expire_pas() throws Exception {
        String recent = book(1);
        String confirmed = book(1);
        jdbc.update("update bookings set status = 'CONFIRMED' where id = ?", confirmed);
        bookedMinutesAgo(confirmed, 30);

        expireUnpaidBookings.expireOverdueBookings();

        assertThat(statusOf(recent)).isEqualTo("AWAITING_PAYMENT");
        assertThat(statusOf(confirmed)).isEqualTo("CONFIRMED");
        assertThat(stock()).isEqualTo("8");
    }
}

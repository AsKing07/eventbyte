package fr.soft_labs.eventbyte.adapters.in.web;

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

/** US-12 de bout en bout : POST /bookings -> Redis (stock) -> Postgres (statut). Docker requis, sinon ignoré. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class BookingIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StringRedisTemplate redis;

    String eventId;
    String ticketId;

    /** Un événement avec un ticket à 3 places ; ids uniques pour ne pas partager le stock Redis entre tests. */
    @BeforeEach
    void createEvent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        eventId = "evt_" + suffix;
        ticketId = "tkt_" + suffix;
        jdbc.update("""
                insert into events (id, title, description, cover, start_at, venue_name, venue_address,
                                    venue_latitude, venue_longitude, updated_at)
                values (?, 'Masterclass', 'd', 'https://img', now() + interval '10 days', 'New Morning', 'Paris',
                        48.87, 2.35, now())""", eventId);
        jdbc.update("insert into tickets (id, event_id, name, price, quantity) values (?, ?, 'Place', 1500, 3)",
                ticketId, eventId);
    }

    String book(int quantity) throws Exception {
        String body = "{ \"items\": [ { \"id\": \"" + ticketId + "\", \"quantity\": " + quantity + " } ] }";
        String json = mvc.perform(post("/bookings").with(jwt().jwt(j -> j.subject("alice")))
                                 .contentType(MediaType.APPLICATION_JSON).content(body))
                         .andExpect(status().isOk())
                         .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    String statusOf(String bookingId) {
        return jdbc.queryForObject("select status from bookings where id = ?", String.class, bookingId);
    }

    @Test
    void reserve_tant_qu_il_reste_des_places_puis_rejette() throws Exception {
        String first = book(2);
        String second = book(2);   // il ne reste qu'une place
        String third = book(1);

        assertThat(statusOf(first)).isEqualTo("RESERVED");
        assertThat(statusOf(second)).isEqualTo("REJECTED");
        assertThat(statusOf(third)).isEqualTo("RESERVED");
        assertThat(redis.opsForValue().get("stock:ticket:" + ticketId)).isEqualTo("0");
    }

    @Test
    void la_reservation_enregistre_l_utilisateur_et_le_prix_de_la_base() throws Exception {
        String id = book(1);

        assertThat(jdbc.queryForObject("select user_id from bookings where id = ?", String.class, id)).isEqualTo("alice");
        assertThat(jdbc.queryForObject("select unit_price from booking_lines where booking_id = ?", Long.class, id))
                .isEqualTo(1500L);
    }
}

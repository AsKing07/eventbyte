package fr.soft_labs.eventbyte.adapters.in.messaging;

import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Décision EB-37 : sur event.updated, les tickets sont fusionnés PAR ID (jamais supprimés puis recréés).
 * Un ticket gardé conserve sa ligne et ses réservations dans le stock ; le nom et le prix d'une réservation
 * restent ceux du moment de l'achat ; un ticket retiré dans Strapi ne fait pas disparaître l'historique
 * (booking_lines n'a volontairement pas de clé étrangère vers tickets).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class TicketIdsStabilityIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RabbitTemplate rabbit;

    @Autowired
    JdbcTemplate jdbc;

    String eventId;
    String ticketId;
    String otherTicketId;

    void publish(String routingKey, String ticketsJson) {
        String json = """
                { "id": "%s", "title": "Concert", "description": "d", "cover": "https://img",
                  "startAt": "2026-12-31T21:00:00.000Z",
                  "venue": { "name": "Le Rex", "address": "Paris", "latitude": 48.87, "longitude": 2.34 },
                  "tickets": %s }
                """.formatted(eventId, ticketsJson);
        rabbit.send("events", routingKey, new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties()));
    }

    String ticket(String id, String name, int quantity, String price) {
        return "{ \"id\": \"%s\", \"name\": \"%s\", \"quantity\": %d, \"price\": %s }".formatted(id, name, quantity, price);
    }

    @BeforeEach
    void eventCreatedInStrapi() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        eventId = "evt_" + suffix;
        ticketId = "tkt_" + suffix;
        otherTicketId = "tkt_vip_" + suffix;
        publish("event.created", "[" + ticket(ticketId, "Standard", 10, "25.00") + "]");
        await().atMost(Duration.ofSeconds(10)).until(() ->
                jdbc.queryForList("select 1 from tickets where id = ?", ticketId).size() == 1);
    }

    String bookTwo() throws Exception {
        String json = mvc.perform(post("/bookings").with(jwt().jwt(j -> j.subject("alice")))
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content("{ \"items\": [ { \"id\": \"" + ticketId + "\", \"quantity\": 2 } ] }"))
                         .andExpect(status().isOk())
                         .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    void un_ticket_modifie_garde_son_id_et_ses_reservations_dans_le_stock() throws Exception {
        String bookingId = bookTwo();

        // L'organisateur renomme le ticket, change son prix et ajoute un VIP
        publish("event.updated", "[" + ticket(ticketId, "Standard tarif 2", 10, "30.00") + ","
                + ticket(otherTicketId, "VIP", 5, "90.00") + "]");
        await().atMost(Duration.ofSeconds(10)).until(() -> Long.valueOf(3000).equals(
                jdbc.queryForObject("select price from tickets where id = ?", Long.class, ticketId)));

        assertThat(jdbc.queryForObject("select count(*) from tickets where id = ?", Integer.class, ticketId)).isEqualTo(1);
        // La réservation garde le nom et le prix du moment de l'achat
        assertThat(jdbc.queryForObject("select name from booking_lines where booking_id = ?", String.class, bookingId))
                .isEqualTo("Standard");
        assertThat(jdbc.queryForObject("select unit_price from booking_lines where booking_id = ?", Long.class, bookingId))
                .isEqualTo(2500L);
        // Les 2 places réservées restent décomptées : 10 - 2
        mvc.perform(get("/events/" + eventId + "/tickets"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[?(@.id == '" + ticketId + "')].quantity").value(8));
    }

    @Test
    void un_ticket_retire_dans_strapi_ne_fait_pas_disparaitre_l_historique() throws Exception {
        String bookingId = bookTwo();

        publish("event.updated", "[" + ticket(otherTicketId, "VIP", 5, "90.00") + "]");
        await().atMost(Duration.ofSeconds(10)).until(() ->
                jdbc.queryForList("select 1 from tickets where id = ?", ticketId).isEmpty());

        assertThat(jdbc.queryForObject("select status from bookings where id = ?", String.class, bookingId))
                .isEqualTo("AWAITING_PAYMENT");
        assertThat(jdbc.queryForObject("select ticket_id from booking_lines where booking_id = ?", String.class, bookingId))
                .isEqualTo(ticketId);
    }
}

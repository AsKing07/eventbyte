package fr.soft_labs.eventbyte.adapters.in.messaging;

import fr.soft_labs.eventbyte.domain.booking.PaymentOutcome;
import fr.soft_labs.eventbyte.ports.in.ConfirmPayment;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Décision EB-31 : l'organisateur supprime dans Strapi un événement qui a des réservations.
 * L'événement disparaît du public et ne se vend plus, mais les réservations existantes sont CONSERVÉES
 * (historique, paiement en cours honoré) : rien n'est annulé ni remboursé automatiquement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class EventDeletionWithBookingsIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RabbitTemplate rabbit;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ConfirmPayment confirmPayment;

    String eventId;
    String ticketId;

    void publish(String routingKey, String json) {
        rabbit.send("events", routingKey, new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties()));
    }

    @BeforeEach
    void eventCreatedInStrapi() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        eventId = "evt_" + suffix;
        ticketId = "tkt_" + suffix;
        publish("event.created", """
                { "id": "%s", "title": "Concert", "description": "d", "cover": "https://img",
                  "startAt": "2026-12-31T21:00:00.000Z",
                  "venue": { "name": "Le Rex", "address": "Paris", "latitude": 48.87, "longitude": 2.34 },
                  "tickets": [{ "id": "%s", "name": "Standard", "quantity": 10, "price": 25.00 }] }
                """.formatted(eventId, ticketId));
        await().atMost(Duration.ofSeconds(10)).until(() ->
                jdbc.queryForList("select 1 from tickets where id = ?", ticketId).size() == 1);
    }

    String book() throws Exception {
        String json = mvc.perform(post("/bookings").with(jwt().jwt(j -> j.subject("alice")))
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content("{ \"items\": [ { \"id\": \"" + ticketId + "\", \"quantity\": 2 } ] }"))
                         .andExpect(status().isOk())
                         .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    void deletedInStrapi() {
        publish("event.deleted", "{ \"id\": \"" + eventId + "\" }");
        await().atMost(Duration.ofSeconds(10)).until(() -> Boolean.TRUE.equals(jdbc.queryForObject(
                "select deleted_at is not null from events where id = ?", Boolean.class, eventId)));
    }

    String statusOf(String bookingId) {
        return jdbc.queryForObject("select status from bookings where id = ?", String.class, bookingId);
    }

    @Test
    void les_reservations_existantes_sont_conservees_avec_leurs_lignes() throws Exception {
        String bookingId = book();

        deletedInStrapi();

        assertThat(statusOf(bookingId)).isEqualTo("AWAITING_PAYMENT");
        assertThat(jdbc.queryForObject("select quantity from booking_lines where booking_id = ?", Integer.class, bookingId))
                .isEqualTo(2);
    }

    @Test
    void l_evenement_supprime_n_est_plus_visible_ni_reservable() throws Exception {
        book();

        deletedInStrapi();

        mvc.perform(get("/events/" + eventId)).andExpect(status().isNotFound());
        mvc.perform(post("/bookings").with(jwt().jwt(j -> j.subject("bob")))
                           .contentType(MediaType.APPLICATION_JSON)
                           .content("{ \"items\": [ { \"id\": \"" + ticketId + "\", \"quantity\": 1 } ] }"))
           .andExpect(status().isBadRequest());   // « Ticket inconnu ou plus en vente »
    }

    @Test
    void un_paiement_en_cours_reste_honore_apres_la_suppression() throws Exception {
        String bookingId = book();
        deletedInStrapi();

        PaymentOutcome outcome = confirmPayment.paymentSucceeded("pi_" + bookingId, bookingId);

        assertThat(outcome).isEqualTo(PaymentOutcome.CONFIRMED);
        assertThat(statusOf(bookingId)).isEqualTo("CONFIRMED");
    }
}

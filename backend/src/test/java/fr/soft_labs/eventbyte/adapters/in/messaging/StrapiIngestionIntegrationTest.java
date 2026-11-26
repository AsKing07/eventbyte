package fr.soft_labs.eventbyte.adapters.in.messaging;

import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * US-2 de bout en bout : un message publié comme le fait Strapi (exchange "events",
 * JSON brut sans content-type) arrive en base. Docker requis, sinon ignoré.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class StrapiIngestionIntegrationTest {

    @Autowired
    RabbitTemplate rabbit;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        // Les réservations (US-12) référencent les événements : on les vide d'abord
        jdbc.update("delete from booking_lines");
        jdbc.update("delete from bookings");
        jdbc.update("delete from tickets");
        jdbc.update("delete from events");
    }

    void publish(String routingKey, String json) {
        // Comme amqplib côté Strapi : corps brut, pas de content-type
        rabbit.send("events", routingKey, new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties()));
    }

    String event(String title, String ticketsJson) {
        return """
                { "id": "evt_it", "title": "%s", "description": "d", "cover": "https://img",
                  "venue": { "name": "Le Rex", "address": "Paris", "latitude": 48.87, "longitude": 2.34 },
                  "tickets": %s }
                """.formatted(title, ticketsJson);
    }

    Integer count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void un_evenement_cree_est_enregistre_avec_son_lieu_et_ses_tickets() {
        publish("event.created", event("Concert", """
                [{ "id": "tkt_1", "name": "Early", "quantity": 100, "price": 25.00 }]"""));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(count("select count(*) from tickets where event_id = 'evt_it' and price = 2500")).isEqualTo(1));
        assertThat(jdbc.queryForObject("select venue_name from events where id = 'evt_it'", String.class)).isEqualTo("Le Rex");
    }

    @Test
    void un_message_recu_deux_fois_ne_cree_pas_de_doublon() {
        String created = event("Concert", """
                [{ "id": "tkt_1", "name": "Early", "quantity": 100, "price": 25.00 }]""");

        publish("event.created", created);
        publish("event.created", created);
        publish("event.updated", event("Concert v2", """
                [{ "id": "tkt_1", "name": "Early", "quantity": 80, "price": 25.00 },
                 { "id": "tkt_2", "name": "VIP", "quantity": 10, "price": 90.00 }]"""));

        // ignoreExceptions : tant que le premier message n'est pas traité, la ligne n'existe pas encore
        // et queryForObject lève une exception (pas une AssertionError, qu'awaitility réessaierait seul)
        await().atMost(Duration.ofSeconds(10)).ignoreExceptions().untilAsserted(() ->
                assertThat(jdbc.queryForObject("select title from events where id = 'evt_it'", String.class)).isEqualTo("Concert v2"));
        assertThat(count("select count(*) from events")).isEqualTo(1);
        assertThat(count("select count(*) from tickets where event_id = 'evt_it'")).isEqualTo(2);
        assertThat(count("select quantity from tickets where id = 'tkt_1'")).isEqualTo(80);
    }

    @Test
    void un_evenement_supprime_est_marque_et_pas_efface() {
        publish("event.created", event("Concert", "[]"));
        await().atMost(Duration.ofSeconds(10)).until(() -> count("select count(*) from events") == 1);

        publish("event.deleted", "{ \"id\": \"evt_it\" }");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(count("select count(*) from events where id = 'evt_it' and deleted_at is not null")).isEqualTo(1));
    }
}

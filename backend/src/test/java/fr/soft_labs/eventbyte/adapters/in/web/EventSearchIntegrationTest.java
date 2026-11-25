package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-7 de bout en bout : message Strapi -> base + Meilisearch -> GET /events/search.
 * Docker requis, sinon ignoré.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class EventSearchIntegrationTest {

    // Le Rex (Paris 2e) ; recherche depuis Châtelet, à ~1,5 km
    static final String LAT = "48.8584", LNG = "2.3470";

    @Autowired
    RabbitTemplate rabbit;

    @Autowired
    MockMvc mvc;

    void publish(String routingKey, String json) {
        rabbit.send("events", routingKey, new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties()));
    }

    static String event(String id, String title, double lat, double lng) {
        return """
                { "id": "%s", "title": "%s", "description": "Soirée", "cover": "https://img",
                  "startAt": "2026-12-31T21:00:00.000Z",
                  "venue": { "name": "Le Rex", "address": "5 bd Poissonnière, Paris", "latitude": %s, "longitude": %s },
                  "tickets": [{ "id": "tkt_%s", "name": "Standard", "quantity": 100, "price": 30.23 }] }
                """.formatted(id, title, lat, lng, id);
    }

    @Test
    void un_evenement_ingere_est_trouve_par_mot_cle_dans_le_rayon() {
        publish("event.created", event("evt_jazz", "Nuit jazz manouche", 48.8707, 2.3475));
        publish("event.created", event("evt_lyon", "Jazz à Lyon", 45.7640, 4.8357));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get("/events/search").param("lat", LAT).param("lng", LNG)
                                .param("radius", "5").param("query", "jazz"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$[*].id").value(hasItem("evt_jazz")))
                   .andExpect(jsonPath("$[*].id").value(not(hasItem("evt_lyon"))))   // hors rayon
                   .andExpect(jsonPath("$[0].lowestPrice").value(30.23)));
    }

    @Test
    void un_evenement_supprime_n_est_plus_trouve() {
        publish("event.created", event("evt_suppr", "Concert éphémère", 48.8707, 2.3475));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get("/events/search").param("lat", LAT).param("lng", LNG)
                                .param("radius", "5").param("query", "éphémère"))
                   .andExpect(jsonPath("$[*].id").value(hasItem("evt_suppr"))));

        publish("event.deleted", "{ \"id\": \"evt_suppr\" }");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get("/events/search").param("lat", LAT).param("lng", LNG)
                                .param("radius", "5").param("query", "éphémère"))
                   .andExpect(jsonPath("$[*].id").value(not(hasItem("evt_suppr")))));
    }
}

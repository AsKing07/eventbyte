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
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-4 de bout en bout : message Strapi -> base + Meilisearch -> GET /events/discover.
 * Docker requis, sinon ignoré.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class EventDiscoveryIntegrationTest {

    // Position par défaut du front (Paris)
    static final String LAT = "48.8585", LNG = "2.3475";

    @Autowired
    RabbitTemplate rabbit;

    @Autowired
    MockMvc mvc;

    void publish(String routingKey, String json) {
        rabbit.send("events", routingKey, new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties()));
    }

    static String event(String id, double lat, double lng, Instant startAt) {
        String date = startAt == null ? "null" : "\"" + startAt + "\"";
        return """
                { "id": "%s", "title": "Découverte %s", "description": "Soirée", "cover": "https://img",
                  "startAt": %s,
                  "venue": { "name": "Le Rex", "address": "5 bd Poissonnière, Paris", "latitude": %s, "longitude": %s },
                  "tickets": [{ "id": "tkt_%s", "name": "Standard", "quantity": 100, "price": 30.23 }] }
                """.formatted(id, id, date, lat, lng, id);
    }

    static Instant inDays(int days) {
        return Instant.now().plus(days, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void renvoie_les_evenements_a_venir_du_rayon_tries_par_date() throws Exception {
        publish("event.created", event("evt_disc_tard", 48.8707, 2.3475, inDays(20)));
        publish("event.created", event("evt_disc_tot", 48.8530, 2.3690, inDays(10)));
        publish("event.created", event("evt_disc_passe", 48.8707, 2.3475, inDays(-1)));
        publish("event.created", event("evt_disc_sans_date", 48.8707, 2.3475, null));
        publish("event.created", event("evt_disc_lyon", 45.7640, 4.8357, inDays(5)));      // ~390 km
        publish("event.created", event("evt_disc_meaux", 48.9601, 2.8788, inDays(5)));     // ~40 km : dans le rayon

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get("/events/discover").param("lat", LAT).param("lng", LNG))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$[*].id").value(hasItem("evt_disc_meaux")))
                   .andExpect(jsonPath("$[*].id").value(hasItem("evt_disc_tot")))
                   .andExpect(jsonPath("$[*].id").value(hasItem("evt_disc_tard")))
                   .andExpect(jsonPath("$[*].id").value(not(hasItem("evt_disc_passe"))))
                   .andExpect(jsonPath("$[*].id").value(not(hasItem("evt_disc_sans_date"))))
                   .andExpect(jsonPath("$[*].id").value(not(hasItem("evt_disc_lyon")))));

        // Ordre chronologique : meaux (J+5), tot (J+10), tard (J+20) — les autres tests peuvent ajouter des événements
        mvc.perform(get("/events/discover").param("lat", LAT).param("lng", LNG))
           .andExpect(jsonPath("$[?(@.id == 'evt_disc_tot')].lowestPrice").value(30.23))
           .andExpect(result -> {
               String body = result.getResponse().getContentAsString();
               int meaux = body.indexOf("evt_disc_meaux"), tot = body.indexOf("evt_disc_tot"), tard = body.indexOf("evt_disc_tard");
               assertThat(meaux).isLessThan(tot);
               assertThat(tot).isLessThan(tard);
           });
    }

    @Test
    void un_evenement_supprime_disparait_de_la_decouverte() {
        publish("event.created", event("evt_disc_suppr", 48.8707, 2.3475, inDays(3)));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get("/events/discover").param("lat", LAT).param("lng", LNG))
                   .andExpect(jsonPath("$[*].id").value(hasItem("evt_disc_suppr"))));

        publish("event.deleted", "{ \"id\": \"evt_disc_suppr\" }");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get("/events/discover").param("lat", LAT).param("lng", LNG))
                   .andExpect(jsonPath("$[*].id").value(not(hasItem("evt_disc_suppr")))));
    }
}

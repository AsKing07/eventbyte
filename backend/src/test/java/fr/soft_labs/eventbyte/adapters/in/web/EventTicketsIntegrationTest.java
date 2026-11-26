package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-11 de bout en bout : Postgres (tickets) + Redis (disponible), route publique. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class EventTicketsIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    /** Ids uniques : le stock Redis n'est pas partagé entre tests. */
    String evenementAvecUnTicket(int quantite) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String eventId = "evt_" + suffix;
        jdbc.update("""
                insert into events (id, title, description, cover, start_at, venue_name, venue_address,
                                    venue_latitude, venue_longitude)
                values (?, 'Concert', 'd', 'https://img', now() + interval '10 days', 'Le Rex', 'Paris', 48.87, 2.34)
                """, eventId);
        jdbc.update("insert into tickets (id, event_id, name, price, quantity) values (?, ?, 'Standard', 2500, ?)",
                "tkt_" + suffix, eventId, quantite);
        return eventId;
    }

    @Test
    void renvoie_les_tickets_sans_authentification() throws Exception {
        String eventId = evenementAvecUnTicket(40);

        mvc.perform(get("/events/" + eventId + "/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventId").value(eventId))
                .andExpect(jsonPath("$[0].price").value(25.0))
                .andExpect(jsonPath("$[0].quantity").value(40));
    }

    @Test
    void un_evenement_supprime_renvoie_404() throws Exception {
        String eventId = evenementAvecUnTicket(40);
        jdbc.update("update events set deleted_at = now() where id = ?", eventId);

        mvc.perform(get("/events/" + eventId + "/tickets")).andExpect(status().isNotFound());
    }

    @Test
    void un_evenement_inconnu_renvoie_404() throws Exception {
        mvc.perform(get("/events/evt_inconnu/tickets")).andExpect(status().isNotFound());
    }
}

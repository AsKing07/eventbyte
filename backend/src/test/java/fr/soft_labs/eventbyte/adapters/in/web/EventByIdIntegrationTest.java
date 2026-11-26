package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-5 de bout en bout sur une vraie base : détail public, 404, et pas de collision avec les autres routes /events/... */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class EventByIdIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    void creerEvenement(String id, String startAt) {
        jdbc.update("""
                INSERT INTO events (id, title, description, cover, start_at, venue_name, venue_address,
                                    venue_latitude, venue_longitude)
                VALUES (?, 'Nuit jazz', 'Trio', 'https://img', CAST(? AS timestamptz), 'Le Rex', 'Paris', 48.87, 2.34)
                """, id, startAt);
        jdbc.update("INSERT INTO tickets (id, event_id, name, price, quantity) VALUES (?, ?, 'Standard', 2500, 10)",
                "tkt_" + id, id);
    }

    @Test
    void renvoie_le_detail_sans_authentification() throws Exception {
        creerEvenement("evt_detail", "2026-12-31 21:00:00+00");

        mvc.perform(get("/events/evt_detail"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value("evt_detail"))
           .andExpect(jsonPath("$.title").value("Nuit jazz"))
           .andExpect(jsonPath("$.startAt").value("2026-12-31T21:00:00Z"))
           .andExpect(jsonPath("$.lowestPrice").value(25.0))
           .andExpect(jsonPath("$.isSoldOut").value(false));
    }

    @Test
    void un_evenement_supprime_sans_date_ou_inconnu_renvoie_404() throws Exception {
        creerEvenement("evt_supprime", "2026-12-31 21:00:00+00");
        jdbc.update("UPDATE events SET deleted_at = now() WHERE id = 'evt_supprime'");
        creerEvenement("evt_sans_date", null);

        mvc.perform(get("/events/evt_supprime")).andExpect(status().isNotFound());
        mvc.perform(get("/events/evt_sans_date")).andExpect(status().isNotFound());
        mvc.perform(get("/events/evt_inconnu")).andExpect(status().isNotFound());
    }

    @Test
    void les_routes_fixes_ne_sont_pas_prises_pour_un_id() throws Exception {
        // /events/liked reste protégé (401) au lieu de chercher un événement « liked » (404)
        mvc.perform(get("/events/liked")).andExpect(status().isUnauthorized());
        // /events/search sans ses paramètres : 400 du contrôleur de recherche, pas 404 « événement search »
        mvc.perform(get("/events/search")).andExpect(status().isBadRequest());
    }
}

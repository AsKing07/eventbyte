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

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-6 de bout en bout : GET /events?ids=… lit la vraie base (Postgres via Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class EventByIdsIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void renvoie_les_evenements_demandes_dans_l_ordre_sans_les_supprimes() throws Exception {
        creerEvenement("evt_ids_a", "Concert A");
        creerEvenement("evt_ids_b", "Concert B");
        creerEvenement("evt_ids_supprime", "Supprimé");
        jdbc.update("UPDATE events SET deleted_at = now() WHERE id = 'evt_ids_supprime'");

        // Route publique : pas de jeton
        mvc.perform(get("/events").param("ids", "evt_ids_b,evt_ids_supprime,evt_inconnu,evt_ids_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("evt_ids_b"))
                .andExpect(jsonPath("$[0].title").value("Concert B"))
                .andExpect(jsonPath("$[1].id").value("evt_ids_a"));
    }

    @Test
    void plus_de_100_ids_renvoie_400() throws Exception {
        String ids = IntStream.rangeClosed(1, 101).mapToObj(i -> "evt_" + i).collect(Collectors.joining(","));

        mvc.perform(get("/events").param("ids", ids))
                .andExpect(status().isBadRequest());
    }

    private void creerEvenement(String id, String titre) {
        jdbc.update("""
                INSERT INTO events (id, title, description, cover, start_at, venue_name, venue_address,
                                    venue_latitude, venue_longitude)
                VALUES (?, ?, '', '', TIMESTAMPTZ '2026-12-31 21:00:00+00', 'Salle', 'Paris', 48.85, 2.35)
                """, id, titre);
    }
}

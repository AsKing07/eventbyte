package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class LikedEventsIntegrationTest {

    private static final String PATH = "/events/liked";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    LikeRepository likes;

    @Test
    void chaque_compte_recoit_uniquement_ses_evenements_likes() throws Exception {
        UserId sabine = new UserId("sabine");
        UserId charbel = new UserId("charbel");
        creerEvenement("evt_concert_liked", "Concert de jazz");
        creerEvenement("evt_theatre_liked", "Pièce de théâtre");
        likes.add(sabine, "evt_concert_liked");
        likes.add(charbel, "evt_theatre_liked");

        mvc.perform(get(PATH).with(jwt().jwt(token -> token.subject(sabine.value()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("evt_concert_liked"))
                .andExpect(jsonPath("$[0].title").value("Concert de jazz"))
                .andExpect(jsonPath("$[0].likedCount").value(1));

        mvc.perform(get(PATH).with(jwt().jwt(token -> token.subject(charbel.value()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("evt_theatre_liked"));
    }

    @Test
    void un_compte_sans_like_recoit_une_liste_vide() throws Exception {
        mvc.perform(get(PATH).with(jwt().jwt(token -> token.subject("sans-like"))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void une_requete_sans_authentification_renvoie_401() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    private void creerEvenement(String id, String titre) {
        jdbc.update("""
                INSERT INTO events (id, title, description, cover, start_at, venue_name, venue_address,
                                    venue_latitude, venue_longitude)
                VALUES (?, ?, '', '', TIMESTAMPTZ '2026-12-31 21:00:00+00', 'Salle', 'Paris', 48.85, 2.35)
                """, id, titre);
    }
}

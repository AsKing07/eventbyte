package fr.soft_labs.eventbyte.adapters.in.web;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class LikeEventIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    LikeRepository likes;

    @Test
    void deux_demandes_du_meme_utilisateur_creent_un_seul_like() throws Exception {
        jdbc.update("""
                INSERT INTO events (id, title, description, cover, venue_name, venue_address,
                                    venue_latitude, venue_longitude)
                VALUES ('evt_like', 'Concert', '', '', 'Salle', 'Paris', 48.8, 2.3)
                """);
        String path = "/events/evt_like/like";

        mvc.perform(post(path)).andExpect(status().isUnauthorized());
        assertThat(likes.countFor("evt_like")).isZero();

        mvc.perform(post(path).with(jwt().jwt(token -> token.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        mvc.perform(post(path).with(jwt().jwt(token -> token.subject("user-123"))))
                .andExpect(status().isOk());

        assertThat(likes.countFor("evt_like")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT user_id FROM likes WHERE event_id = ?",
                String.class, "evt_like")).isEqualTo("user-123");

        mvc.perform(post(path).with(jwt().jwt(token -> token.subject("user-456"))))
                .andExpect(status().isOk());
        assertThat(likes.countFor("evt_like")).isEqualTo(2);
    }

    @Test
    void un_evenement_inconnu_renvoie_404() throws Exception {
        mvc.perform(post("/events/evt_inconnu_like/like").with(jwt()))
                .andExpect(status().isNotFound());
    }
}
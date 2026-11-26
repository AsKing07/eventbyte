package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;
import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class UnlikeEventIntegrationTest {

    private static final String EVENT_ID = "evt_unlike";
    private static final String PATH = "/events/" + EVENT_ID + "/like";
    private static final UserId SABINE = new UserId("sabine");
    private static final UserId CHARBEL = new UserId("charbel");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    LikeRepository likes;

    @BeforeEach
    void creer_evenement() {
        jdbc.update("""
                INSERT INTO events (id, title, description, cover, venue_name, venue_address,
                                    venue_latitude, venue_longitude)
                VALUES (?, 'Concert', '', '', 'Salle', 'Paris', 48.8, 2.3)
                """, EVENT_ID);
    }

    @Test
    void sabine_retire_son_like_et_celui_de_charbel_reste() throws Exception {
        likes.add(SABINE, EVENT_ID);
        likes.add(CHARBEL, EVENT_ID);

        mvc.perform(delete(PATH).with(jwt().jwt(token -> token.subject(SABINE.value()))))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT user_id FROM likes WHERE event_id = ?", String.class, EVENT_ID))
                .containsExactly(CHARBEL.value());
    }

    @Test
    void retirer_un_like_absent_renvoie_200() throws Exception {
        mvc.perform(delete(PATH).with(jwt().jwt(token -> token.subject(SABINE.value()))))
                .andExpect(status().isOk());

        assertThat(likes.countFor(EVENT_ID)).isZero();
    }

    @Test
    void retirer_deux_fois_renvoie_200_et_le_compteur_reste_a_zero() throws Exception {
        likes.add(SABINE, EVENT_ID);

        mvc.perform(delete(PATH).with(jwt().jwt(token -> token.subject(SABINE.value()))))
                .andExpect(status().isOk());
        mvc.perform(delete(PATH).with(jwt().jwt(token -> token.subject(SABINE.value()))))
                .andExpect(status().isOk());

        assertThat(likes.countFor(EVENT_ID)).isZero();
    }

    @Test
    void retirer_un_like_sans_authentification_renvoie_401() throws Exception {
        likes.add(SABINE, EVENT_ID);

        mvc.perform(delete(PATH)).andExpect(status().isUnauthorized());

        assertThat(likes.countFor(EVENT_ID)).isEqualTo(1);
    }
}

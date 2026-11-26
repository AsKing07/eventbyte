package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.support.FakePaymentConfiguration;
import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-15 de bout en bout : réserver puis relire, et ne jamais voir la réservation d'un autre. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class BookingQueryIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    String eventId;
    String ticketId;

    @BeforeEach
    void createEvent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        eventId = "evt_" + suffix;
        ticketId = "tkt_" + suffix;
        jdbc.update("""
                insert into events (id, title, description, cover, start_at, venue_name, venue_address,
                                    venue_latitude, venue_longitude)
                values (?, 'Concert', 'd', 'https://img', now() + interval '10 days', 'Le Rex', 'Paris', 48.87, 2.34)
                """, eventId);
        jdbc.update("insert into tickets (id, event_id, name, price, quantity) values (?, ?, 'Standard', 2500, 10)",
                ticketId, eventId);
    }

    String book(String user) throws Exception {
        String json = mvc.perform(post("/bookings").with(jwt().jwt(j -> j.subject(user)))
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content("{ \"items\": [ { \"id\": \"" + ticketId + "\", \"quantity\": 2 } ] }"))
                         .andExpect(status().isOk())
                         .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    void l_utilisateur_relit_sa_reservation_au_format_du_readme() throws Exception {
        String id = book("alice");

        mvc.perform(get("/bookings/" + id).with(jwt().jwt(j -> j.subject("alice"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(id))
           .andExpect(jsonPath("$.eventId").value(eventId))
           .andExpect(jsonPath("$.bookedAt").isString())
           .andExpect(jsonPath("$.lines[0].id").value(ticketId))
           .andExpect(jsonPath("$.lines[0].name").value("Standard"))
           .andExpect(jsonPath("$.lines[0].quantity").value(2))
           .andExpect(jsonPath("$.lines[0].price").value(25.0))
           .andExpect(jsonPath("$.status").value("awaiting-payment"))
           .andExpect(jsonPath("$.fees").value(0.6))
           .andExpect(jsonPath("$.total").value(50.6))
           .andExpect(jsonPath("$.clientSecret").value(FakePaymentConfiguration.clientSecretFor(id)));
    }

    @Test
    void la_reservation_d_un_autre_ou_inexistante_donne_404() throws Exception {
        String id = book("alice");

        mvc.perform(get("/bookings/" + id).with(jwt().jwt(j -> j.subject("bob"))))
           .andExpect(status().isNotFound());
        mvc.perform(get("/bookings/bk_inconnue").with(jwt().jwt(j -> j.subject("alice"))))
           .andExpect(status().isNotFound());
    }

    @Test
    void la_liste_ne_contient_que_les_reservations_de_l_utilisateur() throws Exception {
        String user = "user-" + UUID.randomUUID();   // isolé des réservations des autres tests
        String first = book(user);
        String second = book(user);
        book("quelqu-un-d-autre");

        mvc.perform(get("/bookings/user").with(jwt().jwt(j -> j.subject(user))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(2))
           .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.containsInAnyOrder(first, second)));
    }

    @Test
    void sans_jeton_401() throws Exception {
        mvc.perform(get("/bookings/user")).andExpect(status().isUnauthorized());
    }
}

package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.config.SecurityConfig;
import fr.soft_labs.eventbyte.config.WebConfig;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.in.GetEventsByIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-6 : contrat HTTP de récupération d'événements par IDs. */
@WebMvcTest(controllers = EventByIdsController.class)
@Import({SecurityConfig.class, WebConfig.class})
class EventByIdsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GetEventsByIds getEventsByIds;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void renvoie_les_evenements_au_format_attendu_et_parse_les_ids_csv() throws Exception {
        Event event = new Event("evt_1", "Nuit jazz", "Trio", "https://img",
                Instant.parse("2026-12-31T21:00:00Z"),
                new Venue("Le Rex", "5 bd Poissonnière", new Coordinates(48.8707, 2.3475)),
                List.of(new Ticket("tkt_1", "Standard", 3023, 10)));
        when(getEventsByIds.getEventsByIds(List.of("evt_1", "evt_2")))
                .thenReturn(List.of(new EventView(event, 3)));

        mvc.perform(get("/events").param("ids", "evt_1, evt_2,evt_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("evt_1"))
                .andExpect(jsonPath("$[0].venue.adresse").value("5 bd Poissonnière"))
                .andExpect(jsonPath("$[0].likedCount").value(3))
                .andExpect(jsonPath("$[0].lowestPrice").value(30.23));

        verify(getEventsByIds).getEventsByIds(List.of("evt_1", "evt_2"));
    }

    @Test
    void ids_manquant_renvoie_400() throws Exception {
        mvc.perform(get("/events")).andExpect(status().isBadRequest());
    }
}

package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.config.SecurityConfig;
import fr.soft_labs.eventbyte.config.WebConfig;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.SearchCriteria;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.in.SearchEvents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-7 : forme de la réponse (contrat du README) et validation des paramètres. Sans token. */
@WebMvcTest(controllers = EventSearchController.class)
@Import({SecurityConfig.class, WebConfig.class})
class EventSearchControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SearchEvents searchEvents;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void renvoie_les_evenements_au_format_du_contrat() throws Exception {
        Event event = new Event("evt_1", "Nuit jazz", "Trio", "https://img", Instant.parse("2026-12-31T21:00:00Z"),
                new Venue("Le Rex", "5 bd Poissonnière", new Coordinates(48.8707, 2.3475)),
                List.of(new Ticket("a", "VIP", 7620, 10), new Ticket("b", "Standard", 3023, 0)));
        when(searchEvents.search(any())).thenReturn(List.of(new EventView(event, 3)));

        mvc.perform(get("/events/search").param("lat", "48.85").param("lng", "2.35")
                        .param("radius", "10000").param("query", "jazz"))   // mètres, comme le front
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value("evt_1"))
           .andExpect(jsonPath("$[0].venue.adresse").value("5 bd Poissonnière"))
           .andExpect(jsonPath("$[0].venue.coordinates.latitude").value(48.8707))
           .andExpect(jsonPath("$[0].likedCount").value(3))
           .andExpect(jsonPath("$[0].isSoldOut").value(false))
           .andExpect(jsonPath("$[0].startAt").value("2026-12-31T21:00:00Z"))
           .andExpect(jsonPath("$[0].lowestPrice").value(30.23))   // euros (décision EB-28)
           .andExpect(jsonPath("$[0].description").value("Trio"));

        verify(searchEvents).search(new SearchCriteria(new Coordinates(48.85, 2.35), 10, "jazz"));
    }

    @Test
    void le_mot_cle_est_facultatif() throws Exception {
        when(searchEvents.search(any())).thenReturn(List.of());

        mvc.perform(get("/events/search").param("lat", "48.85").param("lng", "2.35").param("radius", "10000"))
           .andExpect(status().isOk());

        verify(searchEvents).search(new SearchCriteria(new Coordinates(48.85, 2.35), 10, ""));
    }

    @Test
    void le_rayon_maximal_du_front_100_km_est_accepte() throws Exception {
        when(searchEvents.search(any())).thenReturn(List.of());

        mvc.perform(get("/events/search").param("lat", "48.85").param("lng", "2.35").param("radius", "100000"))
           .andExpect(status().isOk());

        verify(searchEvents).search(new SearchCriteria(new Coordinates(48.85, 2.35), 100, ""));
    }

    @Test
    void un_rayon_invalide_renvoie_400() throws Exception {
        mvc.perform(get("/events/search").param("lat", "48.85").param("lng", "2.35").param("radius", "-1"))
           .andExpect(status().isBadRequest());

        verifyNoInteractions(searchEvents);
    }

    @Test
    void une_position_manquante_renvoie_400() throws Exception {
        mvc.perform(get("/events/search").param("radius", "10"))
           .andExpect(status().isBadRequest());
    }
}

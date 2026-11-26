package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.config.SecurityConfig;
import fr.soft_labs.eventbyte.config.WebConfig;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.in.DiscoverEvents;
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

/** US-4 : forme de la réponse (contrat du README) et validation des paramètres. Sans token. */
@WebMvcTest(controllers = EventDiscoveryController.class)
@Import({SecurityConfig.class, WebConfig.class})
class EventDiscoveryControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DiscoverEvents discoverEvents;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void renvoie_les_evenements_au_format_du_contrat() throws Exception {
        Event event = new Event("evt_1", "Nuit jazz", "Trio", "https://img", Instant.parse("2026-12-31T21:00:00Z"),
                new Venue("Le Rex", "5 bd Poissonnière", new Coordinates(48.8707, 2.3475)),
                List.of(new Ticket("a", "VIP", 7620, 10), new Ticket("b", "Standard", 3023, 0)));
        when(discoverEvents.discover(any())).thenReturn(List.of(new EventView(event, 3)));

        mvc.perform(get("/events/discover").param("lat", "48.8585").param("lng", "2.3475"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value("evt_1"))
           .andExpect(jsonPath("$[0].title").value("Nuit jazz"))
           .andExpect(jsonPath("$[0].cover").value("https://img"))
           .andExpect(jsonPath("$[0].venue.name").value("Le Rex"))
           .andExpect(jsonPath("$[0].venue.adresse").value("5 bd Poissonnière"))
           .andExpect(jsonPath("$[0].venue.address").doesNotExist())
           .andExpect(jsonPath("$[0].venue.coordinates.latitude").value(48.8707))
           .andExpect(jsonPath("$[0].venue.coordinates.longitude").value(2.3475))
           .andExpect(jsonPath("$[0].likedCount").value(3))
           .andExpect(jsonPath("$[0].isSoldOut").value(false))
           .andExpect(jsonPath("$[0].startAt").value("2026-12-31T21:00:00Z"))
           .andExpect(jsonPath("$[0].lowestPrice").value(30.23))   // euros (décision EB-28)
           .andExpect(jsonPath("$[0].description").value("Trio"));

        verify(discoverEvents).discover(new Coordinates(48.8585, 2.3475));
    }

    @Test
    void une_position_hors_bornes_renvoie_400() throws Exception {
        mvc.perform(get("/events/discover").param("lat", "999").param("lng", "2.35"))
           .andExpect(status().isBadRequest());

        verifyNoInteractions(discoverEvents);
    }

    @Test
    void une_position_manquante_ou_mal_formee_renvoie_400() throws Exception {
        mvc.perform(get("/events/discover").param("lng", "2.35"))
           .andExpect(status().isBadRequest());
        mvc.perform(get("/events/discover").param("lat", "abc").param("lng", "2.35"))
           .andExpect(status().isBadRequest());

        verifyNoInteractions(discoverEvents);
    }
}

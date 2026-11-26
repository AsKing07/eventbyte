package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.config.SecurityConfig;
import fr.soft_labs.eventbyte.config.WebConfig;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.in.GetEventById;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventByIdController.class)
@Import({SecurityConfig.class, WebConfig.class})
class EventByIdControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GetEventById getEventById;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void renvoie_le_detail_au_format_attendu_sans_authentification() throws Exception {
        Event event = new Event("evt_1", "Nuit jazz", "Trio", "https://img",
                Instant.parse("2026-12-31T21:00:00Z"),
                new Venue("Le Rex", "5 bd Poissonnière", new Coordinates(48.8707, 2.3475)),
                List.of(new Ticket("tkt_1", "Standard", 3023, 10)));
        when(getEventById.getEventById("evt_1")).thenReturn(new EventView(event, 3));

        mvc.perform(get("/events/evt_1"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "id": "evt_1",
                          "title": "Nuit jazz",
                          "cover": "https://img",
                          "likedCount": 3,
                          "venue": {
                            "name": "Le Rex",
                            "adresse": "5 bd Poissonnière",
                            "coordinates": {"latitude": 48.8707, "longitude": 2.3475}
                          },
                          "isSoldOut": false,
                          "startAt": "2026-12-31T21:00:00Z",
                          "lowestPrice": 30.23,
                          "description": "Trio"
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void renvoie_404_pour_un_evenement_introuvable() throws Exception {
        when(getEventById.getEventById("evt_missing")).thenThrow(new EventNotFoundException("evt_missing"));

        mvc.perform(get("/events/evt_missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

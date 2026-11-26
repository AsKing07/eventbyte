package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.config.SecurityConfig;
import fr.soft_labs.eventbyte.config.WebConfig;
import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
import fr.soft_labs.eventbyte.domain.event.EventTicketAvailability;
import fr.soft_labs.eventbyte.ports.in.GetEventTickets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-11 : contrat REST public, prix en euros et erreur 404 pour un événement invisible. */
@WebMvcTest(controllers = EventTicketsController.class)
@Import({SecurityConfig.class, WebConfig.class})
class EventTicketsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GetEventTickets getEventTickets;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void renvoie_les_tickets_avec_prix_en_euros_et_quantite_disponible_sans_authentification() throws Exception {
        when(getEventTickets.getForEvent("evt_1")).thenReturn(
                List.of(new EventTicketAvailability("tkt_1", "evt_1", "Standard", 3023, 4)));

        mvc.perform(get("/events/evt_1/tickets"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value("tkt_1"))
           .andExpect(jsonPath("$[0].eventId").value("evt_1"))
           .andExpect(jsonPath("$[0].name").value("Standard"))
           .andExpect(jsonPath("$[0].price").value(30.23))
           .andExpect(jsonPath("$[0].quantity").value(4));

        verify(getEventTickets).getForEvent("evt_1");
    }

    @Test
    void renvoie_404_pour_un_evenement_inconnu_ou_supprime() throws Exception {
        when(getEventTickets.getForEvent("evt_hidden")).thenThrow(new EventNotFoundException("evt_hidden"));

        mvc.perform(get("/events/evt_hidden/tickets"))
           .andExpect(status().isNotFound());
    }
}

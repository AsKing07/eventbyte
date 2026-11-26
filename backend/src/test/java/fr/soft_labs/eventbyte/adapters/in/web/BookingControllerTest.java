package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.config.SecurityConfig;
import fr.soft_labs.eventbyte.config.WebConfig;
import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.RequestedItem;
import fr.soft_labs.eventbyte.ports.in.RequestBooking;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@Import({SecurityConfig.class, WebConfig.class})
class BookingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RequestBooking requestBooking;

    @MockitoBean
    JwtDecoder jwtDecoder;

    static final String BODY = """
            { "items": [ { "id": "tkt_std", "quantity": 2 }, { "id": "tkt_vip", "quantity": 1 } ] }
            """;

    @Test
    void sans_token_401() throws Exception {
        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isUnauthorized());

        verifyNoInteractions(requestBooking);
    }

    @Test
    void renvoie_l_id_et_l_utilisateur_vient_du_jwt() throws Exception {
        Booking booking = Booking.request("bk_42", new UserId("user-123"), "evt_1",
                List.of(new BookingLine("tkt_std", "Standard", 2, 2200)), Instant.now()).reserve();
        when(requestBooking.book(any(), anyList())).thenReturn(booking);

        mvc.perform(post("/bookings").with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value("bk_42"));

        verify(requestBooking).book(new UserId("user-123"),
                List.of(new RequestedItem("tkt_std", 2), new RequestedItem("tkt_vip", 1)));
    }

    @Test
    void corps_invalide_400() throws Exception {
        for (String body : List.of("{ \"items\": [] }",
                                   "{ \"items\": [ { \"id\": \"tkt_std\", \"quantity\": 0 } ] }",
                                   "{ \"items\": [ { \"quantity\": 1 } ] }")) {
            mvc.perform(post("/bookings").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body))
               .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(requestBooking);
    }

    @Test
    void ticket_inconnu_400() throws Exception {
        when(requestBooking.book(any(), anyList())).thenThrow(new IllegalArgumentException("Ticket inconnu"));

        mvc.perform(post("/bookings").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isBadRequest());
    }
}

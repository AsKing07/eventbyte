package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.config.SecurityConfig;
import fr.soft_labs.eventbyte.config.WebConfig;
import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.in.SubscribeToBookingUpdates;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingStreamController.class)
@Import({SecurityConfig.class, WebConfig.class})
class BookingStreamControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SubscribeToBookingUpdates bookingUpdates;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void sans_token_le_flux_est_refuse() throws Exception {
        mvc.perform(get("/bookings/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingUpdates);
    }

    @Test
    void le_flux_est_abonne_au_sub_du_jwt() throws Exception {
        when(bookingUpdates.subscribe(eq(new UserId("user-123")), any())).thenReturn(() -> {});

        mvc.perform(get("/bookings/stream")
                        .with(jwt().jwt(token -> token.subject("user-123")))
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());

        verify(bookingUpdates).subscribe(eq(new UserId("user-123")), any());
    }
}

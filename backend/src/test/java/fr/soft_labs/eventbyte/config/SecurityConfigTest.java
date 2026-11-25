package fr.soft_labs.eventbyte.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-3 : sans token valide, 401 sur les routes authentifiées ;
 * l'identité vient du sub du JWT ; les routes publiques restent ouvertes.
 */
@WebMvcTest(controllers = SecurityTestController.class)
@Import({SecurityConfig.class, WebConfig.class})
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    // Remplace l'appel à Keycloak : un token inconnu est rejeté comme le ferait le vrai décodeur
    @MockitoBean
    JwtDecoder jwtDecoder;

    @ParameterizedTest(name = "{0} {1} sans token -> 401")
    @CsvSource({
            "GET,    /events/liked",
            "DELETE, /events/evt_1/like",
            "POST,   /bookings",
            "GET,    /bookings/user"
    })
    void route_authentifiee_sans_token_renvoie_401(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void token_invalide_renvoie_401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("signature invalide"));

        mvc.perform(get("/bookings/user").header("Authorization", "Bearer pas-un-vrai-token"))
           .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1} avec token -> l'utilisateur est le sub")
    @CsvSource({
            "GET,    /events/liked",
            "DELETE, /events/evt_1/like",
            "POST,   /bookings",
            "GET,    /bookings/user"
    })
    void route_authentifiee_avec_token_utilise_le_sub(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path).with(jwt().jwt(j -> j.subject("user-123"))))
           .andExpect(status().isOk())
           .andExpect(content().string("user-123"));
    }

    @ParameterizedTest(name = "GET {0} sans token -> 200")
    @CsvSource({"/events/discover", "/events/evt_1", "/events/evt_1/tickets"})
    void route_publique_accessible_sans_token(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void preflight_cors_du_front_autorise() throws Exception {
        mvc.perform(options("/bookings")
                   .header("Origin", "http://localhost:5173")
                   .header("Access-Control-Request-Method", "POST")
                   .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
           .andExpect(status().isOk())
           .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void origine_inconnue_refusee() throws Exception {
        mvc.perform(options("/bookings")
                   .header("Origin", "http://evil.example")
                   .header("Access-Control-Request-Method", "POST"))
           .andExpect(status().isForbidden());
    }
}

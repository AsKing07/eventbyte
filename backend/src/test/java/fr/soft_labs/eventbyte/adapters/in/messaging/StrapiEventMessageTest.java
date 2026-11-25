package fr.soft_labs.eventbyte.adapters.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrapiEventMessageTest {

    final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /** Forme exacte produite par resultToWebHook dans Strapi (lifecycles.ts). */
    static final String STRAPI_JSON = """
            {
              "id": "evt_0193",
              "title": "Nuit électro",
              "description": "Line-up complet",
              "cover": "https://res.cloudinary.com/demo/image/upload/cover.jpg",
              "venue": { "name": "Le Rex", "address": "5 bd Poissonnière, Paris", "latitude": 48.8707, "longitude": 2.3475 },
              "tickets": [
                { "id": "tkt_a", "name": "Early Bird", "quantity": 120, "price": 30.23 },
                { "id": "tkt_b", "name": "VIP", "quantity": 20, "price": 0 }
              ]
            }
            """;

    Event read(String json) throws Exception {
        return mapper.readValue(json, StrapiEventMessage.class).toDomain();
    }

    @Test
    void convertit_le_message_strapi_en_evenement() throws Exception {
        Event event = read(STRAPI_JSON);

        assertThat(event.id()).isEqualTo("evt_0193");
        assertThat(event.venue().address()).isEqualTo("5 bd Poissonnière, Paris");
        assertThat(event.venue().coordinates().latitude()).isEqualTo(48.8707);
        assertThat(event.startDate()).isEmpty();
        assertThat(event.tickets()).containsExactly(
                new Ticket("tkt_a", "Early Bird", 3023, 120),   // 30.23 € -> 3023 centimes
                new Ticket("tkt_b", "VIP", 0, 20));             // ticket gratuit
    }

    @Test
    void un_prix_en_euros_avec_plus_de_deux_decimales_est_arrondi_au_centime() throws Exception {
        Event event = read(STRAPI_JSON.replace("\"price\": 30.23", "\"price\": 19.995"));

        assertThat(event.tickets().get(0).priceInCents()).isEqualTo(2000);
    }

    @Test
    void lit_startAt_si_strapi_l_envoie() throws Exception {
        Event event = read(STRAPI_JSON.replace("\"cover\"", "\"startAt\": \"2026-12-31T21:00:00.000Z\", \"cover\""));

        assertThat(event.startDate()).contains(Instant.parse("2026-12-31T21:00:00Z"));
    }

    @Test
    void ignore_les_champs_inconnus() throws Exception {
        assertThat(read(STRAPI_JSON.replace("\"title\"", "\"nouveauChamp\": 1, \"title\"")).title())
                .isEqualTo("Nuit électro");
    }

    @Test
    void refuse_un_message_sans_lieu() {
        String sansLieu = STRAPI_JSON.replaceAll("\"venue\": \\{[^}]*},", "");

        assertThatThrownBy(() -> read(sansLieu)).isInstanceOf(NullPointerException.class);
    }
}

package fr.soft_labs.eventbyte.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Format d'une réservation tel que le schéma zod du front l'attend (flux SSE et GET /bookings). */
class BookingResponseTest {

    final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();

    Booking reserved() {
        return Booking.request("bk_1", new UserId("alice"), "evt_1",
                List.of(new BookingLine("tkt_std", "Standard", 2, 2200), new BookingLine("tkt_vip", "VIP", 1, 3550)),
                Instant.parse("2026-11-26T10:00:00Z")).reserve();
    }

    @Test
    void montants_en_euros_frais_compris() {
        BookingResponse response = BookingResponse.from(reserved());

        assertThat(response.lines().get(0).price()).isEqualByComparingTo("22.00");
        assertThat(response.fees()).isEqualByComparingTo("0.90");     // 3 tickets x 0,30 €
        assertThat(response.total()).isEqualByComparingTo("80.40");   // 79,50 € + 0,90 €
    }

    @Test
    void chaque_statut_a_le_libelle_du_readme() {
        assertThat(Arrays.stream(BookingStatus.values()).map(BookingResponse::apiStatus))
                .containsExactly("requested", "reserved", "rejected", "awaiting-payment", "expired", "confirmed");
    }

    @Test
    void client_secret_absent_du_json_tant_qu_il_n_existe_pas_puis_present() throws Exception {
        String sansSecret = json.writeValueAsString(BookingResponse.from(reserved()));
        String avecSecret = json.writeValueAsString(BookingResponse.from(reserved().awaitPayment("pi_1_secret_x")));

        assertThat(sansSecret).doesNotContain("clientSecret").contains("\"status\":\"reserved\"");
        assertThat(avecSecret).contains("\"clientSecret\":\"pi_1_secret_x\"").contains("\"status\":\"awaiting-payment\"");
    }
}

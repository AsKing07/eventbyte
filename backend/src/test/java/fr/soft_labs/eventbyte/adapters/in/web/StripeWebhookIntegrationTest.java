package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.support.FakePaymentConfiguration;
import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-17 de bout en bout : POST /bookings, puis webhook signé comme Stripe -> confirmed. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class StripeWebhookIntegrationTest {

    static final String WEBHOOK = "/payments/stripe/webhook";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    String ticketId;

    @BeforeEach
    void createEvent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String eventId = "evt_" + suffix;
        ticketId = "tkt_" + suffix;
        jdbc.update("""
                insert into events (id, title, description, cover, start_at, venue_name, venue_address,
                                    venue_latitude, venue_longitude)
                values (?, 'Concert', 'd', 'https://img', now() + interval '10 days', 'Le Rex', 'Paris', 48.87, 2.34)
                """, eventId);
        jdbc.update("insert into tickets (id, event_id, name, price, quantity) values (?, ?, 'Standard', 2500, 10)",
                ticketId, eventId);
    }

    String book() throws Exception {
        String json = mvc.perform(post("/bookings").with(jwt().jwt(j -> j.subject("alice")))
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content("{ \"items\": [ { \"id\": \"" + ticketId + "\", \"quantity\": 1 } ] }"))
                         .andExpect(status().isOk())
                         .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    static String paymentSucceeded(String paymentIntentId, String bookingId) {
        return """
                { "id": "evt_stripe_%s", "type": "payment_intent.succeeded",
                  "data": { "object": { "id": "%s", "object": "payment_intent",
                                        "metadata": { "bookingId": "%s" } } } }
                """.formatted(paymentIntentId, paymentIntentId, bookingId);
    }

    /** En-tête Stripe-Signature : t=<horodatage>,v1=HMAC-SHA256("<t>.<payload>", secret), comme Stripe. */
    static String sign(String payload, String secret) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String v1 = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + v1;
    }

    void sendWebhook(String payload, String signature, int expectedStatus) throws Exception {
        mvc.perform(post(WEBHOOK).contentType(MediaType.APPLICATION_JSON)
                                 .header("Stripe-Signature", signature).content(payload))
           .andExpect(status().is(expectedStatus));
    }

    String statusOf(String bookingId) {
        return jdbc.queryForObject("select status from bookings where id = ?", String.class, bookingId);
    }

    @Test
    void un_webhook_signe_confirme_la_reservation_sans_jwt() throws Exception {
        String bookingId = book();
        String payload = paymentSucceeded("pi_" + bookingId, bookingId);

        sendWebhook(payload, sign(payload, FakePaymentConfiguration.WEBHOOK_SECRET), 200);

        assertThat(statusOf(bookingId)).isEqualTo("CONFIRMED");
    }

    @Test
    void le_meme_webhook_recu_deux_fois_est_traite_une_seule_fois() throws Exception {
        String bookingId = book();
        String payload = paymentSucceeded("pi_" + bookingId, bookingId);

        sendWebhook(payload, sign(payload, FakePaymentConfiguration.WEBHOOK_SECRET), 200);
        sendWebhook(payload, sign(payload, FakePaymentConfiguration.WEBHOOK_SECRET), 200);

        assertThat(statusOf(bookingId)).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject("select count(*) from processed_payments where payment_intent_id = ?",
                Integer.class, "pi_" + bookingId)).isEqualTo(1);
    }

    @Test
    void une_signature_invalide_est_refusee_et_rien_ne_change() throws Exception {
        String bookingId = book();
        String payload = paymentSucceeded("pi_" + bookingId, bookingId);

        sendWebhook(payload, sign(payload, "whsec_pirate"), 400);

        assertThat(statusOf(bookingId)).isEqualTo("AWAITING_PAYMENT");
    }
}

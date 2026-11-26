package fr.soft_labs.eventbyte.adapters.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import fr.soft_labs.eventbyte.domain.booking.PaymentOutcome;
import fr.soft_labs.eventbyte.ports.in.ConfirmPayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * US-17 : POST /payments/stripe/webhook, appelé par Stripe (pas de JWT).
 * La signature Stripe-Signature, calculée avec STRIPE_WEBHOOK_SECRET, prouve que l'appel vient de Stripe.
 * En local : {@code stripe listen --forward-to localhost:3000/payments/stripe/webhook}.
 * <p>
 * Le JSON est lu à la main plutôt qu'avec les classes Stripe : celles-ci renvoient un objet vide
 * si la version d'API du webhook diffère de celle de la librairie.
 */
@RestController
class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    static final String PAYMENT_SUCCEEDED = "payment_intent.succeeded";

    private final ConfirmPayment confirmPayment;
    private final ObjectMapper json;
    private final String webhookSecret;

    StripeWebhookController(ConfirmPayment confirmPayment, ObjectMapper json,
                            @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.confirmPayment = confirmPayment;
        this.json = json;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/payments/stripe/webhook")
    ResponseEntity<Void> receive(@RequestBody String payload,
                                 @RequestHeader(name = "Stripe-Signature", required = false) String signature)
            throws IOException {
        if (!StringUtils.hasText(webhookSecret)) {
            log.error("Webhook Stripe reçu mais STRIPE_WEBHOOK_SECRET n'est pas configurée");
            return ResponseEntity.status(503).build();   // Stripe réessaiera
        }
        if (signature == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Webhook.Signature.verifyHeader(payload, signature, webhookSecret, Webhook.DEFAULT_TOLERANCE);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook Stripe refusé : signature invalide");
            return ResponseEntity.badRequest().build();
        }

        JsonNode event = json.readTree(payload);
        if (!PAYMENT_SUCCEEDED.equals(event.path("type").asText())) {
            return ResponseEntity.ok().build();   // autres événements : rien à faire, mais pas de nouvel envoi
        }
        JsonNode paymentIntent = event.path("data").path("object");
        String paymentIntentId = paymentIntent.path("id").asText(null);
        String bookingId = paymentIntent.path("metadata").path("bookingId").asText(null);
        if (paymentIntentId == null) {
            return ResponseEntity.badRequest().build();
        }

        PaymentOutcome outcome = confirmPayment.paymentSucceeded(paymentIntentId, bookingId);
        log.info("Paiement {} (réservation {}) : {}", paymentIntentId, bookingId, outcome);
        return ResponseEntity.ok().build();
    }
}

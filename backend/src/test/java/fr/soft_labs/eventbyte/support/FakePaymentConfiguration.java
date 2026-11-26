package fr.soft_labs.eventbyte.support;

import fr.soft_labs.eventbyte.ports.out.PaymentGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * Les tests d'intégration n'appellent jamais Stripe : ce faux remplace l'adapter Stripe
 * et renvoie un clientSecret prévisible. Il fixe aussi le secret de webhook pour signer les appels de test.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakePaymentConfiguration {

    public static final String WEBHOOK_SECRET = "whsec_test_eventbyte";

    public static String clientSecretFor(String bookingId) {
        return "pi_" + bookingId + "_secret_test";
    }

    @Bean
    @Primary
    PaymentGateway fakePaymentGateway() {
        return new PaymentGateway() {
            @Override
            public String createPaymentIntent(String bookingId, long amountInCents) {
                return clientSecretFor(bookingId);
            }

            @Override
            public void refund(String paymentIntentId) {
            }
        };
    }

    @Bean
    DynamicPropertyRegistrar stripeWebhookSecret() {
        return registry -> registry.add("stripe.webhook-secret", () -> WEBHOOK_SECRET);
    }
}

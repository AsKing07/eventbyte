package fr.soft_labs.eventbyte.support;

import fr.soft_labs.eventbyte.ports.out.PaymentGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Les tests d'intégration n'appellent jamais Stripe : ce faux remplace l'adapter Stripe
 * et renvoie un clientSecret prévisible.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakePaymentConfiguration {

    public static String clientSecretFor(String bookingId) {
        return "pi_" + bookingId + "_secret_test";
    }

    @Bean
    @Primary
    PaymentGateway fakePaymentGateway() {
        return (bookingId, amountInCents) -> clientSecretFor(bookingId);
    }
}

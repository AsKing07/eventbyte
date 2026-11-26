package fr.soft_labs.eventbyte.adapters.out.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import fr.soft_labs.eventbyte.domain.booking.PaymentUnavailableException;
import fr.soft_labs.eventbyte.ports.out.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class StripePaymentGateway implements PaymentGateway {

    private final String secretKey;
    private final String currency;

    StripePaymentGateway(@Value("${stripe.secret-key:}") String secretKey,
                         @Value("${stripe.currency:eur}") String currency) {
        this.secretKey = secretKey;
        this.currency = currency;
    }

    @Override
    public String createPaymentIntent(String bookingId, long amountInCents) {
        if (!StringUtils.hasText(secretKey)) {
            throw new PaymentUnavailableException("STRIPE_SECRET_KEY n'est pas configurée");
        }
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency)
                // Lu par le webhook (US-17) pour retrouver la réservation payée
                .putMetadata("bookingId", bookingId)
                // Le front utilise le Payment Element : Stripe choisit les moyens de paiement activés
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build())
                .build();
        RequestOptions options = RequestOptions.builder()
                .setApiKey(secretKey)              // clé par requête : pas d'état global Stripe.apiKey
                .setIdempotencyKey("booking-" + bookingId)   // un nouvel essai ne crée pas un second paiement
                .build();
        try {
            return PaymentIntent.create(params, options).getClientSecret();
        } catch (StripeException e) {
            throw new PaymentUnavailableException("Stripe a refusé la création du paiement : " + e.getMessage(), e);
        }
    }
}

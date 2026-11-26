package fr.soft_labs.eventbyte.domain.booking;

/** Le paiement n'a pas pu être préparé (Stripe injoignable, clé absente...) : les places ont été rendues. */
public class PaymentUnavailableException extends RuntimeException {

    public PaymentUnavailableException(String message) {
        super(message);
    }

    public PaymentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

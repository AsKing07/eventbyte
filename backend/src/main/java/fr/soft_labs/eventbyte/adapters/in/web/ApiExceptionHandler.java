package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.booking.PaymentUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les erreurs de validation du domaine en 400 pour tous les contrôleurs
 * (ex. rayon négatif, latitude hors bornes). Les paramètres manquants ou mal typés
 * sont déjà renvoyés en 400 par Spring.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Stripe injoignable ou non configuré : les places ont déjà été rendues, le client peut réessayer. */
    @ExceptionHandler(PaymentUnavailableException.class)
    ProblemDetail paymentUnavailable(PaymentUnavailableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}

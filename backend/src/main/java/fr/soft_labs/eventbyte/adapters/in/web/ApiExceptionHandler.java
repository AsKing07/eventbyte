package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.event.EventNotFoundException;
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

    @ExceptionHandler(EventNotFoundException.class)
    ProblemDetail notFound(EventNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}

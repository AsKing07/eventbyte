package fr.soft_labs.eventbyte.adapters.in.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injecte l'utilisateur connecté dans un contrôleur, lu depuis le {@code sub} du JWT.
 * Usage : ajouter un paramètre {@code @CurrentUser UserId userId} à une méthode de contrôleur.
 * À n'utiliser que sur une route authentifiée (voir SecurityConfig).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}

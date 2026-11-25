package fr.soft_labs.eventbyte.adapters.in.web;

import fr.soft_labs.eventbyte.domain.UserId;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Résout les paramètres {@code @CurrentUser UserId} à partir du JWT validé par Spring Security.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserId.class.equals(parameter.getParameterType());
    }

    @Override
    public UserId resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            // Route mal déclarée dans SecurityConfig : on refuse plutôt que d'inventer un utilisateur
            throw new AuthenticationCredentialsNotFoundException("Aucun JWT pour cette requête");
        }
        return new UserId(jwt.getSubject());
    }
}

package fr.soft_labs.eventbyte.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Configuration de départ donnée par le sujet : API stateless, JWT Keycloak lu s'il est présent.
 * Toutes les routes sont ouvertes pour l'instant : US-3 (EB-9) exige un token valide
 * sur les routes authentifiées et ajoute le CORS pour le front (localhost:5173).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().permitAll())
            .oauth2ResourceServer(o -> o.jwt(j -> {}));
        return http.build();
    }
}

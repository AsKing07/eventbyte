package fr.soft_labs.eventbyte.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * US-3 : les JWT Keycloak sont validés (signature, expiration, issuer) via l'issuer-uri.
 * Routes authentifiées d'après le contrat du README : 401 sans token valide.
 * L'utilisateur s'obtient avec {@code @CurrentUser UserId}, jamais depuis le corps de la requête.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(a -> a
                // Préflight CORS du navigateur
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Routes authentifiées du README (à déclarer avant GET /events/{id})
                .requestMatchers(HttpMethod.GET, "/events/liked").authenticated()
                .requestMatchers("/events/*/like").authenticated()
                .requestMatchers("/bookings", "/bookings/**").authenticated()
                // Routes publiques : discover, détail, lots, recherche, tickets
                .requestMatchers(HttpMethod.GET, "/events", "/events/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                // Tout le reste est fermé par défaut (le webhook Stripe sera ouvert avec US-17)
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${eventbyte.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Cache-Control", "Last-Event-ID"));
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}

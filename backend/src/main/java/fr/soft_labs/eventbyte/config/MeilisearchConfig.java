package fr.soft_labs.eventbyte.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client Meilisearch. Il ne se connecte qu'au premier appel :
 * l'application démarre même si Meilisearch est arrêté.
 */
@Configuration
public class MeilisearchConfig {

    @Bean
    Client meilisearchClient(@Value("${meilisearch.host}") String host,
                             @Value("${meilisearch.api-key}") String apiKey) {
        return new Client(new Config(host, apiKey));
    }
}

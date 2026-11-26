package fr.soft_labs.eventbyte.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Infra partagée par les tests d'intégration : un seul jeu de conteneurs
 * tant que le contexte Spring est réutilisé entre classes de test.
 * Usage : {@code @SpringBootTest @Import(TestcontainersConfiguration.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import(FakePaymentConfiguration.class)   // aucun test d'intégration n'appelle Stripe
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:15");
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitmq() {
        return new RabbitMQContainer("rabbitmq:3-management-alpine");
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redis() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    }

    @Bean
    MeilisearchContainer meilisearch() {
        return new MeilisearchContainer();
    }

    /** Pas de @ServiceConnection pour Meilisearch dans Spring Boot : on branche nos propriétés à la main. */
    @Bean
    DynamicPropertyRegistrar meilisearchProperties(MeilisearchContainer meilisearch) {
        return registry -> {
            registry.add("meilisearch.host", meilisearch::url);
            registry.add("meilisearch.api-key", () -> MeilisearchContainer.MASTER_KEY);
        };
    }
}

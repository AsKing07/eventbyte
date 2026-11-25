package fr.soft_labs.eventbyte;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-1 : l'application démarre avec Postgres, RabbitMQ et Redis,
 * et le schéma est créé par Flyway puis validé par Hibernate (ddl-auto: validate).
 * Les conteneurs sont lancés par Testcontainers : il faut Docker, rien d'autre.
 * Sans Docker, le test est ignoré au lieu d'échouer.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ApplicationContextTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void le_contexte_demarre_et_flyway_cree_le_schema() {
        Integer tables = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name in ('events', 'tickets')",
                Integer.class);

        assertThat(tables).isEqualTo(2);
    }
}

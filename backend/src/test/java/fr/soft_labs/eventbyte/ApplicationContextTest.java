package fr.soft_labs.eventbyte;

import fr.soft_labs.eventbyte.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-1 : l'application démarre avec Postgres, RabbitMQ et Redis,
 * et le schéma est créé par Flyway puis validé par Hibernate (ddl-auto: validate).
 * Les conteneurs viennent de TestcontainersConfiguration, partagée avec les autres tests d'intégration.
 * Sans Docker, le test est ignoré au lieu d'échouer.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApplicationContextTest {

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

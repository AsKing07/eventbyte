package fr.soft_labs.eventbyte.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/** Meilisearch pour les tests, même version que le docker-compose. */
public class MeilisearchContainer extends GenericContainer<MeilisearchContainer> {

    public static final String MASTER_KEY = "test-master-key-eventbyte";

    public MeilisearchContainer() {
        super(DockerImageName.parse("getmeili/meilisearch:v1.7"));
        withEnv("MEILI_MASTER_KEY", MASTER_KEY);
        withEnv("MEILI_NO_ANALYTICS", "true");
        withExposedPorts(7700);
        waitingFor(Wait.forHttp("/health").forStatusCode(200));
    }

    public String url() {
        return "http://" + getHost() + ":" + getMappedPort(7700);
    }
}

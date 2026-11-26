package fr.soft_labs.eventbyte.adapters.out.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.Settings;
import fr.soft_labs.eventbyte.domain.event.DiscoveryCriteria;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.SearchCriteria;
import fr.soft_labs.eventbyte.ports.out.EventSearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Index Meilisearch des événements. Un document = les champs cherchables + la position "_geo"
 * + "startAt" en secondes epoch (absent si l'événement n'a pas de date).
 * Meilisearch traite les écritures en tâche de fond : un événement est cherchable
 * quelques millisecondes après son ingestion.
 */
@Component
class MeilisearchEventIndex implements EventSearchIndex {

    private static final Logger LOG = LoggerFactory.getLogger(MeilisearchEventIndex.class);
    static final int MAX_RESULTS = 50;

    private final Client client;
    private final String indexName;
    private final ObjectMapper objectMapper;

    MeilisearchEventIndex(Client client,
                          @Value("${meilisearch.events-index}") String indexName,
                          ObjectMapper objectMapper) {
        this.client = client;
        this.indexName = indexName;
        this.objectMapper = objectMapper;
    }

    private Index index() throws Exception {
        return client.index(indexName);
    }

    /**
     * Déclare les champs cherchables et le filtre géographique. Idempotent : rejoué à chaque démarrage.
     * Si Meilisearch est arrêté, l'application démarre quand même (la recherche échouera jusqu'à son retour).
     */
    @EventListener(ApplicationReadyEvent.class)
    void configureIndex() {
        try {
            Settings settings = new Settings();
            settings.setSearchableAttributes(new String[]{"title", "description", "venueName", "venueAddress"});
            settings.setFilterableAttributes(new String[]{"_geo", "startAt"});
            settings.setSortableAttributes(new String[]{"_geo", "startAt"});
            index().updateSettings(settings);
        } catch (Exception e) {
            LOG.warn("Configuration de l'index Meilisearch '{}' impossible : {}", indexName, e.getMessage());
        }
    }

    @Override
    public void index(Event event) {
        Map<String, Object> document = new HashMap<>(Map.of(
                "id", event.id(),
                "title", event.title(),
                "description", event.description(),
                "venueName", event.venue().name(),
                "venueAddress", event.venue().address(),
                "_geo", Map.of("lat", event.venue().coordinates().latitude(),
                               "lng", event.venue().coordinates().longitude())));
        if (event.startAt() != null) {
            document.put("startAt", event.startAt().getEpochSecond());
        }
        try {
            index().addDocuments(objectMapper.writeValueAsString(List.of(document)), "id");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Document Meilisearch non sérialisable : " + event.id(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Indexation Meilisearch impossible : " + event.id(), e);
        }
    }

    @Override
    public void remove(String eventId) {
        try {
            index().deleteDocument(eventId);
        } catch (Exception e) {
            throw new IllegalStateException("Suppression Meilisearch impossible : " + eventId, e);
        }
    }

    @Override
    public List<String> search(SearchCriteria criteria) {
        SearchRequest.SearchRequestBuilder request = SearchRequest.builder()
                .q(criteria.query())
                .filter(new String[]{geoRadius(criteria)})
                .attributesToRetrieve(new String[]{"id"})
                .limit(MAX_RESULTS);
        if (!criteria.hasQuery()) {
            // Sans mot-clé, pas de pertinence : les plus proches d'abord
            request.sort(new String[]{String.format(Locale.ROOT, "_geoPoint(%s, %s):asc",
                    criteria.center().latitude(), criteria.center().longitude())});
        }
        return ids(request.build());
    }

    /** Découverte (EB-36) : rayon fixe, événements à venir, triés par date de début. */
    @Override
    public List<String> discover(DiscoveryCriteria criteria) {
        return ids(SearchRequest.builder()
                .q("")
                .filter(new String[]{discoveryFilter(criteria)})
                .sort(new String[]{"startAt:asc"})
                .attributesToRetrieve(new String[]{"id"})
                .limit(DiscoveryCriteria.MAX_RESULTS)
                .build());
    }

    private List<String> ids(SearchRequest request) {
        try {
            return index().search(request).getHits().stream()
                    .map(hit -> Objects.toString(hit.get("id"), null))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Recherche Meilisearch impossible", e);
        }
    }

    /** Filtre Meilisearch : _geoRadius(lat, lng, distance en mètres). Locale.ROOT pour le point décimal. */
    static String geoRadius(SearchCriteria criteria) {
        return geoRadius(criteria.center().latitude(), criteria.center().longitude(), criteria.radiusKm());
    }

    /** Filtre de la découverte : dans le rayon ET à venir (un document sans startAt ne passe pas le filtre). */
    static String discoveryFilter(DiscoveryCriteria criteria) {
        return geoRadius(criteria.center().latitude(), criteria.center().longitude(), DiscoveryCriteria.RADIUS_KM)
                + " AND startAt >= " + criteria.startingFrom().getEpochSecond();
    }

    private static String geoRadius(double latitude, double longitude, double radiusKm) {
        return String.format(Locale.ROOT, "_geoRadius(%s, %s, %d)", latitude, longitude, Math.round(radiusKm * 1000));
    }
}

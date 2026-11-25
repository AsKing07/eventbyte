package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.SearchCriteria;

import java.util.List;

/**
 * Index de recherche plein texte + géographique (Meilisearch).
 * Il ne sert qu'à trouver des ids : la base reste la source de vérité des données affichées.
 */
public interface EventSearchIndex {

    /** Ajoute ou remplace l'événement dans l'index. */
    void index(Event event);

    /** Retire l'événement de l'index ; sans effet s'il n'y est pas. */
    void remove(String eventId);

    /** Ids des événements correspondants, dans l'ordre de pertinence. */
    List<String> search(SearchCriteria criteria);
}

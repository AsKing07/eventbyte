package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.SearchCriteria;

import java.util.List;

/** US-7 : chercher par mot-clé dans un rayon en km. */
public interface SearchEvents {

    /** Résultats par pertinence (ou par distance sans mot-clé). */
    List<EventView> search(SearchCriteria criteria);
}

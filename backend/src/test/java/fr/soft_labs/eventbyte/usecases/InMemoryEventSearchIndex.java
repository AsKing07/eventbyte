package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.DiscoveryCriteria;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.SearchCriteria;
import fr.soft_labs.eventbyte.ports.out.EventSearchIndex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Faux index : garde les événements indexés et renvoie des ids préparés par le test. */
class InMemoryEventSearchIndex implements EventSearchIndex {

    final Map<String, Event> indexed = new LinkedHashMap<>();
    final List<String> removed = new ArrayList<>();
    List<String> nextResults = List.of();
    SearchCriteria lastCriteria;
    DiscoveryCriteria lastDiscovery;

    @Override
    public void index(Event event) {
        indexed.put(event.id(), event);
    }

    @Override
    public void remove(String eventId) {
        indexed.remove(eventId);
        removed.add(eventId);
    }

    @Override
    public List<String> search(SearchCriteria criteria) {
        lastCriteria = criteria;
        return nextResults;
    }

    @Override
    public List<String> discover(DiscoveryCriteria criteria) {
        lastDiscovery = criteria;
        return nextResults;
    }
}

package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.domain.event.SearchCriteria;
import fr.soft_labs.eventbyte.ports.in.SearchEvents;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import fr.soft_labs.eventbyte.ports.out.EventSearchIndex;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Meilisearch trouve les ids (texte + rayon), la base fournit les données à jour
 * (prix, stock, likes). On garde l'ordre de pertinence de l'index.
 */
public class SearchEventsUseCase implements SearchEvents {

    private final EventSearchIndex index;
    private final EventReadModel events;

    public SearchEventsUseCase(EventSearchIndex index, EventReadModel events) {
        this.index = Objects.requireNonNull(index);
        this.events = Objects.requireNonNull(events);
    }

    @Override
    public List<EventView> search(SearchCriteria criteria) {
        List<String> ids = index.search(criteria);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            rank.putIfAbsent(ids.get(i), i);
        }
        return events.findVisibleByIds(ids).stream()
                // Le front exige une date (startAt non nul) : un événement sans date ne peut pas être affiché
                .filter(view -> view.event().startAt() != null)
                .sorted(Comparator.comparingInt(view -> rank.getOrDefault(view.event().id(), Integer.MAX_VALUE)))
                .toList();
    }
}

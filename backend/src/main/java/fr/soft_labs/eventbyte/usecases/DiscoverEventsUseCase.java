package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.DiscoveryCriteria;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.ports.in.DiscoverEvents;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import fr.soft_labs.eventbyte.ports.out.EventSearchIndex;

import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Meilisearch trouve les ids (rayon + date, triés par date), la base fournit les données à jour.
 * On garde l'ordre de l'index et on revérifie la date sur la donnée de la base, source de vérité.
 */
public class DiscoverEventsUseCase implements DiscoverEvents {

    private final EventSearchIndex index;
    private final EventReadModel events;
    private final Clock clock;

    public DiscoverEventsUseCase(EventSearchIndex index, EventReadModel events, Clock clock) {
        this.index = Objects.requireNonNull(index);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public List<EventView> discover(Coordinates position) {
        DiscoveryCriteria criteria = new DiscoveryCriteria(position, clock.instant());
        List<String> ids = index.discover(criteria);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            rank.putIfAbsent(ids.get(i), i);
        }
        return events.findVisibleByIds(ids).stream()
                // Écarte aussi les événements sans date, que le front ne sait pas afficher
                .filter(view -> criteria.isUpcoming(view.event()))
                .sorted(Comparator.comparingInt(view -> rank.getOrDefault(view.event().id(), Integer.MAX_VALUE)))
                .toList();
    }
}

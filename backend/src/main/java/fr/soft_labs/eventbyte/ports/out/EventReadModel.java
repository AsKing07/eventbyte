package fr.soft_labs.eventbyte.ports.out;

import fr.soft_labs.eventbyte.domain.event.EventView;

import java.util.Collection;
import java.util.List;

/**
 * Lecture des événements visibles du public (non supprimés), avec leurs valeurs calculées.
 * Réutilisable par discover, détail, lots et likes (US-4, 5, 6, 10).
 */
public interface EventReadModel {

    /** Événements trouvés parmi ces ids, sans ordre garanti ; les ids inconnus ou supprimés sont ignorés. */
    List<EventView> findVisibleByIds(Collection<String> ids);
}

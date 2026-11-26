package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.event.EventView;

import java.util.List;

/** US-10 (EB-16) : un utilisateur connecté retrouve les événements qu'il a likés. */
public interface ListLikedEvents {

    /**
     * Événements likés par cet utilisateur et encore visibles (ni supprimés, ni sans date),
     * du like le plus récent au plus ancien.
     */
    List<EventView> listLikedEvents(UserId userId);
}

package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.event.EventView;

import java.util.List;

public interface ListLikedEvents {

    List<EventView> listLikedEvents(UserId userId);
}

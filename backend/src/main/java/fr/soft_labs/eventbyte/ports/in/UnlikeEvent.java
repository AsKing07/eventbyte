package fr.soft_labs.eventbyte.ports.in;

import fr.soft_labs.eventbyte.domain.UserId;

public interface UnlikeEvent {

    void unlike(UserId userId, String eventId);
}

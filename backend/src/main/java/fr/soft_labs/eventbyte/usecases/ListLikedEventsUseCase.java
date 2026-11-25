package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.event.EventView;
import fr.soft_labs.eventbyte.ports.in.ListLikedEvents;
import fr.soft_labs.eventbyte.ports.out.EventReadModel;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;

import java.util.List;
import java.util.Objects;

public class ListLikedEventsUseCase implements ListLikedEvents {

    private final LikeRepository likes;
    private final EventReadModel events;

    public ListLikedEventsUseCase(LikeRepository likes, EventReadModel events) {
        this.likes = Objects.requireNonNull(likes);
        this.events = Objects.requireNonNull(events);
    }

    @Override
    public List<EventView> listLikedEvents(UserId userId) {
        Objects.requireNonNull(userId, "L'utilisateur est obligatoire");
        return events.findVisibleByIds(likes.findEventIdsByUser(userId)).stream()
                .filter(view -> view.event().startAt() != null)
                .toList();
    }
}

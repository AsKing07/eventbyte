package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.in.UnlikeEvent;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;

import java.util.Objects;

public class UnlikeEventUseCase implements UnlikeEvent {

    private final LikeRepository likes;

    public UnlikeEventUseCase(LikeRepository likes) {
        this.likes = Objects.requireNonNull(likes);
    }

    @Override
    public void unlike(UserId userId, String eventId) {
        Objects.requireNonNull(userId, "L'utilisateur est obligatoire");
        likes.remove(userId, eventId);
    }
}

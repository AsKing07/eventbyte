package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;

import java.util.HashSet;
import java.util.Set;

/** Faux repository en mémoire : teste les likes sans base de données. */
class InMemoryLikeRepository implements LikeRepository {

    record Like(UserId userId, String eventId) {}

    final Set<Like> likes = new HashSet<>();

    @Override
    public void add(UserId userId, String eventId) {
        likes.add(new Like(userId, eventId));
    }

    @Override
    public long countFor(String eventId) {
        return likes.stream().filter(like -> like.eventId().equals(eventId)).count();
    }
}

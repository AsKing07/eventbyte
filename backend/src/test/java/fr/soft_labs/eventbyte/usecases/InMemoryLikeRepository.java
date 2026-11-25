package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;

import java.util.HashSet;
import java.util.List;
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
    public List<String> findEventIdsByUser(UserId userId) {
        return likes.stream()
                .filter(like -> like.userId().equals(userId))
                .map(Like::eventId)
                .toList();
    }

    @Override
    public long countFor(String eventId) {
        return likes.stream().filter(like -> like.eventId().equals(eventId)).count();
    }

    @Override
    public java.util.Map<String, Long> countByEvent(java.util.Collection<String> eventIds) {
        return likes.stream()
                .filter(like -> eventIds.contains(like.eventId()))
                .collect(java.util.stream.Collectors.groupingBy(Like::eventId, java.util.stream.Collectors.counting()));
    }
}

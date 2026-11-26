package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Faux repository en mémoire : teste les likes sans base de données. Garde l'ordre des likes. */
class InMemoryLikeRepository implements LikeRepository {

    record Like(UserId userId, String eventId) {}

    final Set<Like> likes = new LinkedHashSet<>();

    @Override
    public void add(UserId userId, String eventId) {
        likes.add(new Like(userId, eventId));
    }

    @Override
    public void remove(UserId userId, String eventId) {
        likes.remove(new Like(userId, eventId));
    }

    /** Du plus récent au plus ancien, comme ORDER BY liked_at DESC. */
    @Override
    public List<String> findEventIdsByUser(UserId userId) {
        List<String> ids = new ArrayList<>(likes.stream()
                .filter(like -> like.userId().equals(userId))
                .map(Like::eventId)
                .toList());
        Collections.reverse(ids);
        return ids;
    }

    @Override
    public long countFor(String eventId) {
        return likes.stream().filter(like -> like.eventId().equals(eventId)).count();
    }

    @Override
    public Map<String, Long> countByEvent(Collection<String> eventIds) {
        return likes.stream()
                .filter(like -> eventIds.contains(like.eventId()))
                .collect(Collectors.groupingBy(Like::eventId, Collectors.counting()));
    }
}

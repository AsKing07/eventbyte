package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
class JdbcLikeRepository implements LikeRepository {

    private final JdbcTemplate jdbc;

    JdbcLikeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void add(UserId userId, String eventId) {
        jdbc.update("""
                INSERT INTO likes (user_id, event_id)
                VALUES (?, ?)
                ON CONFLICT (user_id, event_id) DO NOTHING
                """, userId.value(), eventId);
    }

    @Override
    public void remove(UserId userId, String eventId) {
        jdbc.update("DELETE FROM likes WHERE user_id = ? AND event_id = ?",
                userId.value(), eventId);
    }

    @Override
    public List<String> findEventIdsByUser(UserId userId) {
        // event_id en second critère : ordre stable même pour deux likes à la même seconde
        return jdbc.queryForList("""
                SELECT event_id FROM likes
                WHERE user_id = ?
                ORDER BY liked_at DESC, event_id
                """, String.class, userId.value());
    }

    @Override
    public long countFor(String eventId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM likes WHERE event_id = ?", Long.class, eventId);
    }

    @Override
    public Map<String, Long> countByEvent(Collection<String> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", Collections.nCopies(eventIds.size(), "?"));
        Map<String, Long> counts = new HashMap<>();
        jdbc.query("SELECT event_id, count(*) AS total FROM likes WHERE event_id IN (" + placeholders
                        + ") GROUP BY event_id",
                rs -> { counts.put(rs.getString("event_id"), rs.getLong("total")); },
                eventIds.toArray());
        return counts;
    }
}

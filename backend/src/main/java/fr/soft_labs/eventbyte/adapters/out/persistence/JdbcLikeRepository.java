package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.ports.out.LikeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
    public long countFor(String eventId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM likes WHERE event_id = ?", Long.class, eventId);
    }

    @Override
    public java.util.Map<String, Long> countByEvent(java.util.Collection<String> eventIds) {
        if (eventIds.isEmpty()) {
            return java.util.Map.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(eventIds.size(), "?"));
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        jdbc.query("SELECT event_id, count(*) AS total FROM likes WHERE event_id IN (" + placeholders
                        + ") GROUP BY event_id",
                rs -> { counts.put(rs.getString("event_id"), rs.getLong("total")); },
                eventIds.toArray());
        return counts;
    }
}

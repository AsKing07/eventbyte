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
    public long countFor(String eventId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM likes WHERE event_id = ?", Long.class, eventId);
    }
}
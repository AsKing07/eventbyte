package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Table "bookings" (migration V2). */
@Entity
@Table(name = "bookings")
class BookingEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "booked_at", nullable = false, columnDefinition = "timestamptz")
    private Instant bookedAt;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_lines", joinColumns = @JoinColumn(name = "booking_id"))
    private List<BookingLineEmbeddable> lines = new ArrayList<>();

    protected BookingEntity() {
    }

    BookingEntity(String id, String userId, String eventId, Instant bookedAt, List<BookingLineEmbeddable> lines) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.bookedAt = bookedAt;
        this.lines = new ArrayList<>(lines);
    }

    String getId() { return id; }
    String getUserId() { return userId; }
    String getEventId() { return eventId; }
    BookingStatus getStatus() { return status; }
    Instant getBookedAt() { return bookedAt; }
    String getClientSecret() { return clientSecret; }
    List<BookingLineEmbeddable> getLines() { return lines; }

    void setStatus(BookingStatus status) { this.status = status; }
    void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

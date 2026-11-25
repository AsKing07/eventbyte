package fr.soft_labs.eventbyte.adapters.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Table "events" (migration V1). Entité JPA : reste dans l'adapter, jamais dans le domaine. */
@Entity
@Table(name = "events")
class EventEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String cover;

    @Column(name = "start_at", columnDefinition = "timestamptz")
    private Instant startAt;

    @Column(name = "venue_name", nullable = false)
    private String venueName;

    @Column(name = "venue_address", nullable = false)
    private String venueAddress;

    @Column(name = "venue_latitude", nullable = false)
    private double venueLatitude;

    @Column(name = "venue_longitude", nullable = false)
    private double venueLongitude;

    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private Instant deletedAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketEntity> tickets = new ArrayList<>();

    protected EventEntity() {
    }

    EventEntity(String id) {
        this.id = id;
    }

    String getId() { return id; }
    String getTitle() { return title; }
    String getDescription() { return description; }
    String getCover() { return cover; }
    Instant getStartAt() { return startAt; }
    String getVenueName() { return venueName; }
    String getVenueAddress() { return venueAddress; }
    double getVenueLatitude() { return venueLatitude; }
    double getVenueLongitude() { return venueLongitude; }
    Instant getDeletedAt() { return deletedAt; }
    List<TicketEntity> getTickets() { return tickets; }

    void setTitle(String title) { this.title = title; }
    void setDescription(String description) { this.description = description; }
    void setCover(String cover) { this.cover = cover; }
    void setStartAt(Instant startAt) { this.startAt = startAt; }
    void setVenueName(String venueName) { this.venueName = venueName; }
    void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }
    void setVenueLatitude(double venueLatitude) { this.venueLatitude = venueLatitude; }
    void setVenueLongitude(double venueLongitude) { this.venueLongitude = venueLongitude; }
    void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

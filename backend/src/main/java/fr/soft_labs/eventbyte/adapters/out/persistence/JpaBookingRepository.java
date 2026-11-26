package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.domain.UserId;
import fr.soft_labs.eventbyte.domain.booking.Booking;
import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.booking.BookingStatus;
import fr.soft_labs.eventbyte.ports.out.BookingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class JpaBookingRepository implements BookingRepository {

    private final BookingJpaRepository jpa;
    private final Clock clock;

    JpaBookingRepository(BookingJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    /** Les lignes sont écrites à la création seulement : ensuite seuls le statut et le clientSecret évoluent. */
    @Override
    @Transactional
    public void save(Booking booking) {
        BookingEntity entity = jpa.findById(booking.id()).orElseGet(() -> new BookingEntity(
                booking.id(),
                booking.userId().value(),
                booking.eventId(),
                booking.bookedAt(),
                booking.lines().stream()
                        .map(l -> new BookingLineEmbeddable(l.ticketId(), l.name(), l.quantity(), l.unitPriceInCents()))
                        .toList()));
        entity.setStatus(booking.status());
        entity.setClientSecret(booking.clientSecret());
        entity.setUpdatedAt(clock.instant());
        jpa.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> findById(String bookingId) {
        return jpa.findById(bookingId).map(JpaBookingRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findUnpaidBookingsPastDeadline(Instant cutoff) {
        return jpa.findByStatusInAndBookedAtLessThanEqualOrderByBookedAtAsc(
                        List.of(BookingStatus.RESERVED, BookingStatus.AWAITING_PAYMENT), cutoff)
                .stream().map(JpaBookingRepository::toDomain).toList();
    }

    @Override
    @Transactional
    public int expireIfUnpaidAndBookedBefore(String bookingId, Instant cutoff, Instant updatedAt) {
        return jpa.expireIfUnpaidAndBookedBefore(bookingId,
                List.of(BookingStatus.RESERVED, BookingStatus.AWAITING_PAYMENT),
                BookingStatus.EXPIRED, cutoff, updatedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findExpiredBookingsPendingStockReturn() {
        return jpa.findByStatusAndStockReturnedFalseOrderByUpdatedAtAsc(BookingStatus.EXPIRED)
                .stream().map(JpaBookingRepository::toDomain).toList();
    }

    @Override
    @Transactional
    public void markStockReturned(String bookingId) {
        if (jpa.markStockReturned(bookingId, BookingStatus.EXPIRED) == 1) {
            return;
        }
        BookingEntity current = jpa.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException(
                        "Réservation introuvable pendant la restitution du stock : " + bookingId));
        if (current.getStatus() != BookingStatus.EXPIRED || !current.isStockReturned()) {
            throw new IllegalStateException("Restitution du stock non enregistrée pour la réservation " + bookingId);
        }
    }

    @Override
    @Transactional
    public boolean updateIfStatus(Booking booking, BookingStatus expectedStatus) {
        return jpa.updateIfStatus(booking.id(), expectedStatus, booking.status(),
                booking.clientSecret(), clock.instant()) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findByUser(UserId userId) {
        return jpa.findByUserIdOrderByBookedAtDesc(userId.value()).stream()
                .map(JpaBookingRepository::toDomain)
                .toList();
    }

    static Booking toDomain(BookingEntity e) {
        return new Booking(
                e.getId(),
                new UserId(e.getUserId()),
                e.getEventId(),
                e.getBookedAt(),
                e.getLines().stream()
                        .map(l -> new BookingLine(l.getTicketId(), l.getName(), l.getQuantity(), l.getUnitPrice()))
                        .toList(),
                e.getStatus(),
                e.getClientSecret());
    }
}

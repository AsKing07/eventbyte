package fr.soft_labs.eventbyte.domain.booking;

import fr.soft_labs.eventbyte.domain.UserId;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Réservation de tickets pour UN événement. Montants en centimes.
 * Les transitions de statut sont les seules façons de la faire évoluer.
 *
 * @param clientSecret secret Stripe du PaymentIntent, présent à partir de awaiting-payment (US-16)
 */
public record Booking(String id,
                      UserId userId,
                      String eventId,
                      Instant bookedAt,
                      List<BookingLine> lines,
                      BookingStatus status,
                      String clientSecret) {

    /** Frais de service : 0,30 € par ticket, comme le faux backend du front (décision EB-34). */
    public static final long FEE_PER_TICKET_IN_CENTS = 30;

    public Booking {
        Objects.requireNonNull(id, "L'identifiant de la réservation est obligatoire");
        Objects.requireNonNull(userId, "L'utilisateur est obligatoire");
        Objects.requireNonNull(eventId, "L'événement est obligatoire");
        Objects.requireNonNull(bookedAt, "La date de réservation est obligatoire");
        Objects.requireNonNull(status, "Le statut est obligatoire");
        lines = List.copyOf(Objects.requireNonNull(lines, "Les lignes sont obligatoires"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Une réservation contient au moins un ticket");
        }
        Set<String> tickets = new HashSet<>();
        for (BookingLine line : lines) {
            if (!tickets.add(line.ticketId())) {
                throw new IllegalArgumentException("Ticket présent deux fois dans la réservation : " + line.ticketId());
            }
        }
    }

    /** Nouvelle demande de réservation, avant toute vérification du stock. */
    public static Booking request(String id, UserId userId, String eventId, List<BookingLine> lines, Instant now) {
        return new Booking(id, userId, eventId, now, lines, BookingStatus.REQUESTED, null);
    }

    public Booking reserve() {
        return from(BookingStatus.REQUESTED, BookingStatus.RESERVED);
    }

    public Booking reject() {
        return from(BookingStatus.REQUESTED, BookingStatus.REJECTED);
    }

    private Booking from(BookingStatus expected, BookingStatus next) {
        if (status != expected) {
            throw new IllegalStateException("Réservation " + id + " : passage " + status + " -> " + next + " impossible");
        }
        return new Booking(id, userId, eventId, bookedAt, lines, next, clientSecret);
    }

    public int ticketCount() {
        return lines.stream().mapToInt(BookingLine::quantity).sum();
    }

    public long feesInCents() {
        return FEE_PER_TICKET_IN_CENTS * ticketCount();
    }

    /** Total payé : prix des tickets + frais. */
    public long totalInCents() {
        return lines.stream().mapToLong(BookingLine::subtotalInCents).sum() + feesInCents();
    }

    public boolean belongsTo(UserId user) {
        return userId.equals(user);
    }
}

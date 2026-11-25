package fr.soft_labs.eventbyte.domain.event;

import java.util.Objects;

/**
 * Événement tel que le public le voit (discover, détail, recherche, likes),
 * avec les valeurs calculées du contrat d'API : prix le plus bas, complet, nombre de likes.
 *
 * @param likedCount nombre de likes (non négatif) ; 0 si l'événement n'a aucun like
 */
public record EventView(Event event, long likedCount) {

    public EventView {
        Objects.requireNonNull(event, "L'événement est obligatoire");
        if (likedCount < 0) {
            throw new IllegalArgumentException("Nombre de likes négatif");
        }
    }

    /** Prix du ticket le moins cher, en centimes ; 0 si l'événement n'a aucun ticket. */
    public long lowestPriceInCents() {
        return event.tickets().stream().mapToLong(Ticket::priceInCents).min().orElse(0);
    }

    /**
     * Complet quand plus aucun ticket n'est disponible.
     */
    public boolean soldOut() {
        return event.tickets().stream().allMatch(ticket -> ticket.quantity() == 0);
    }
}

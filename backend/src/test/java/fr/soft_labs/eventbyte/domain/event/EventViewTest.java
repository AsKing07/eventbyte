package fr.soft_labs.eventbyte.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventViewTest {

    static EventView view(Ticket... tickets) {
        Event event = new Event("evt_1", "Concert", "d", "https://img", Instant.parse("2026-12-31T21:00:00Z"),
                new Venue("Le Rex", "Paris", new Coordinates(48.87, 2.34)), List.of(tickets));
        return new EventView(event, 0);
    }

    @Test
    void le_prix_le_plus_bas_est_celui_du_ticket_le_moins_cher() {
        assertThat(view(new Ticket("a", "VIP", 7620, 10), new Ticket("b", "Standard", 3023, 50)).lowestPriceInCents())
                .isEqualTo(3023);
    }

    @Test
    void un_evenement_sans_ticket_a_un_prix_de_zero_et_est_complet() {
        EventView view = view();

        assertThat(view.lowestPriceInCents()).isZero();
        assertThat(view.soldOut()).isTrue();
    }

    @Test
    void complet_seulement_quand_aucun_ticket_n_a_de_stock() {
        assertThat(view(new Ticket("a", "VIP", 7620, 0), new Ticket("b", "Standard", 3023, 1)).soldOut()).isFalse();
        assertThat(view(new Ticket("a", "VIP", 7620, 0), new Ticket("b", "Standard", 3023, 0)).soldOut()).isTrue();
    }

    @Test
    void refuse_un_nombre_de_likes_negatif() {
        assertThatThrownBy(() -> new EventView(view().event(), -1)).isInstanceOf(IllegalArgumentException.class);
    }
}

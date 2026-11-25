package fr.soft_labs.eventbyte.domain.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    static Venue venue() {
        return new Venue("Zénith", "211 av. Jean Jaurès, Paris", new Coordinates(48.8939, 2.3933));
    }

    @Test
    void un_evenement_sans_date_est_accepte() {
        Event event = new Event("evt_1", "Concert", "desc", "https://img", null, venue(), List.of());

        assertThat(event.startDate()).isEmpty();
    }

    @Test
    void la_liste_de_tickets_est_copiee() {
        List<Ticket> tickets = new ArrayList<>(List.of(new Ticket("tkt_1", "Early", 2500, 10)));
        Event event = new Event("evt_1", "Concert", "desc", "https://img", null, venue(), tickets);

        tickets.clear();

        assertThat(event.tickets()).hasSize(1);
    }

    @Test
    void refuse_deux_tickets_avec_le_meme_id() {
        List<Ticket> tickets = List.of(new Ticket("tkt_1", "Early", 2500, 10), new Ticket("tkt_1", "VIP", 9000, 5));

        assertThatThrownBy(() -> new Event("evt_1", "Concert", "d", "c", null, venue(), tickets))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_evenement_sans_id() {
        assertThatThrownBy(() -> new Event(" ", "Concert", "d", "c", null, venue(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_prix_ou_une_quantite_negatifs() {
        assertThatThrownBy(() -> new Ticket("tkt_1", "Early", -1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Ticket("tkt_1", "Early", 100, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_des_coordonnees_hors_bornes() {
        assertThatThrownBy(() -> new Coordinates(91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordinates(0, 181)).isInstanceOf(IllegalArgumentException.class);
    }
}

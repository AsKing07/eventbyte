package fr.soft_labs.eventbyte.usecases;

import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.domain.event.Coordinates;
import fr.soft_labs.eventbyte.domain.event.Event;
import fr.soft_labs.eventbyte.domain.event.Ticket;
import fr.soft_labs.eventbyte.domain.event.Venue;
import fr.soft_labs.eventbyte.ports.out.TicketStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IngestEventUseCaseTest {

    static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    InMemoryEventRepository repository;
    InMemoryEventSearchIndex searchIndex;
    RecordingStock stock;
    IngestEventUseCase ingest;

    /** Faux stock : retient les tickets dont le stock a été invalidé. */
    static class RecordingStock implements TicketStock {
        final List<String> refreshed = new ArrayList<>();

        @Override
        public boolean tryTake(List<BookingLine> lines) {
            return true;
        }

        @Override
        public void giveBack(List<BookingLine> lines) {
        }

        @Override
        public void refresh(Collection<String> ticketIds) {
            refreshed.addAll(ticketIds);
        }
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryEventRepository();
        searchIndex = new InMemoryEventSearchIndex();
        stock = new RecordingStock();
        ingest = new IngestEventUseCase(repository, searchIndex, stock, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    static Event concert(String title, Ticket... tickets) {
        return new Event("evt_1", title, "desc", "https://img",
                null, new Venue("Zénith", "Paris", new Coordinates(48.89, 2.39)), List.of(tickets));
    }

    @Test
    void un_evenement_cree_est_enregistre_avec_son_lieu_et_ses_tickets() {
        ingest.upsert(concert("Concert", new Ticket("tkt_1", "Early", 2500, 100)));

        Event saved = repository.find("evt_1").orElseThrow();
        assertThat(saved.venue().name()).isEqualTo("Zénith");
        assertThat(saved.tickets()).containsExactly(new Ticket("tkt_1", "Early", 2500, 100));
    }

    @Test
    void un_message_recu_deux_fois_ne_cree_pas_de_doublon() {
        Event event = concert("Concert", new Ticket("tkt_1", "Early", 2500, 100));

        ingest.upsert(event);
        ingest.upsert(event);

        assertThat(repository.events).hasSize(1);
        assertThat(repository.find("evt_1")).contains(event);
    }

    @Test
    void une_mise_a_jour_remplace_l_etat_de_l_evenement() {
        ingest.upsert(concert("Concert", new Ticket("tkt_1", "Early", 2500, 100)));

        ingest.upsert(concert("Concert complet", new Ticket("tkt_2", "VIP", 9000, 10)));

        Event saved = repository.find("evt_1").orElseThrow();
        assertThat(saved.title()).isEqualTo("Concert complet");
        assertThat(saved.tickets()).extracting(Ticket::id).containsExactly("tkt_2");
    }

    @Test
    void une_suppression_est_logique_et_datee() {
        ingest.upsert(concert("Concert"));

        ingest.delete("evt_1");

        assertThat(repository.isDeleted("evt_1")).isTrue();
        assertThat(repository.deletedAt).containsEntry("evt_1", NOW);
    }

    @Test
    void une_suppression_recue_deux_fois_garde_la_premiere_date() {
        ingest.upsert(concert("Concert"));
        ingest.delete("evt_1");

        IngestEventUseCase later = new IngestEventUseCase(repository, searchIndex, stock,
                Clock.fixed(NOW.plusSeconds(60), ZoneOffset.UTC));
        later.delete("evt_1");

        assertThat(repository.deletedAt).containsEntry("evt_1", NOW);
    }

    @Test
    void supprimer_un_evenement_inconnu_ne_fait_rien() {
        ingest.delete("evt_inconnu");

        assertThat(repository.events).isEmpty();
        assertThat(repository.deletedAt).isEmpty();
    }

    @Test
    void un_message_arrive_apres_la_suppression_ne_ressuscite_pas_l_evenement() {
        ingest.upsert(concert("Concert"));
        ingest.delete("evt_1");
        int savesAvant = repository.saves;

        ingest.upsert(concert("Concert (ancien message)"));

        assertThat(repository.saves).isEqualTo(savesAvant);
        assertThat(repository.find("evt_1").orElseThrow().title()).isEqualTo("Concert");
        assertThat(searchIndex.indexed).doesNotContainKey("evt_1");
    }

    // ---------- US-7 : l'index de recherche suit chaque message ----------

    @Test
    void un_evenement_cree_ou_modifie_est_indexe_dans_son_dernier_etat() {
        ingest.upsert(concert("Concert"));
        ingest.upsert(concert("Concert v2"));

        assertThat(searchIndex.indexed.get("evt_1").title()).isEqualTo("Concert v2");
    }

    @Test
    void un_evenement_supprime_est_retire_de_l_index() {
        ingest.upsert(concert("Concert"));

        ingest.delete("evt_1");

        assertThat(searchIndex.indexed).doesNotContainKey("evt_1");
        assertThat(searchIndex.removed).containsExactly("evt_1");
    }

    // ---------- US-13 / EB-30 : le stock suit les quantités Strapi ----------

    @Test
    void une_mise_a_jour_strapi_fait_recalculer_le_stock_de_ses_tickets() {
        ingest.upsert(concert("Concert", new Ticket("tkt_1", "Early", 2500, 100), new Ticket("tkt_2", "VIP", 9000, 10)));

        assertThat(stock.refreshed).containsExactlyInAnyOrder("tkt_1", "tkt_2");
    }

    @Test
    void un_message_ignore_ne_touche_pas_au_stock() {
        ingest.upsert(concert("Concert", new Ticket("tkt_1", "Early", 2500, 100)));
        ingest.delete("evt_1");
        stock.refreshed.clear();

        ingest.upsert(concert("Concert (ancien message)", new Ticket("tkt_1", "Early", 2500, 5)));

        assertThat(stock.refreshed).isEmpty();
    }
}

package fr.soft_labs.eventbyte.adapters.out.persistence;

import fr.soft_labs.eventbyte.ports.out.ProcessedPayments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Table "processed_payments" (migration V4) : la clé primaire garantit un seul traitement par paiement. */
@Repository
class JdbcProcessedPayments implements ProcessedPayments {

    private final JdbcTemplate jdbc;

    JdbcProcessedPayments(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean claim(String paymentIntentId) {
        // Atomique même si Stripe envoie deux fois en parallèle : une seule insertion réussit
        return jdbc.update("""
                INSERT INTO processed_payments (payment_intent_id)
                VALUES (?)
                ON CONFLICT (payment_intent_id) DO NOTHING
                """, paymentIntentId) == 1;
    }

    @Override
    public void release(String paymentIntentId) {
        jdbc.update("DELETE FROM processed_payments WHERE payment_intent_id = ?", paymentIntentId);
    }
}

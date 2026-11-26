-- US-17 (EB-23) : paiements Stripe déjà traités.
-- Stripe renvoie un webhook tant qu'il n'a pas reçu de 2xx : la clé primaire évite un double traitement.
CREATE TABLE processed_payments (
    payment_intent_id VARCHAR(255) PRIMARY KEY,
    processed_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

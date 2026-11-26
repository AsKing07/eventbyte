-- Réservations (US-12). Montants en centimes ; total et frais sont recalculés à partir des lignes.
CREATE TABLE bookings (
    id            VARCHAR(36)  PRIMARY KEY,
    user_id       VARCHAR(255) NOT NULL,                 -- sub du JWT Keycloak
    event_id      VARCHAR(64)  NOT NULL REFERENCES events (id),
    status        VARCHAR(20)  NOT NULL,                 -- REQUESTED, RESERVED, REJECTED, AWAITING_PAYMENT, EXPIRED, CONFIRMED
    booked_at     TIMESTAMPTZ  NOT NULL,
    client_secret VARCHAR(255),                          -- Stripe, à partir de AWAITING_PAYMENT (US-16)
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_bookings_status ON bookings (status);

-- Lignes : prix unitaire figé au moment de la réservation.
-- Pas de clé étrangère vers tickets : un ticket retiré dans Strapi ne doit pas effacer l'historique.
CREATE TABLE booking_lines (
    booking_id VARCHAR(36)  NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    ticket_id  VARCHAR(64)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    quantity   INTEGER      NOT NULL CHECK (quantity > 0),
    unit_price BIGINT       NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (booking_id, ticket_id)
);

CREATE INDEX idx_booking_lines_ticket_id ON booking_lines (ticket_id);

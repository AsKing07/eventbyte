-- Événements reçus de Strapi (US-2). L'id est celui de Strapi (evt_...).
-- La venue est stockée à plat : elle n'existe pas sans son événement.
CREATE TABLE events (
    id              VARCHAR(64)      PRIMARY KEY,
    title           VARCHAR(255)     NOT NULL,
    description     TEXT             NOT NULL,
    cover           VARCHAR(1024)    NOT NULL,
    start_at        TIMESTAMPTZ,                 -- absent du message Strapi pour l'instant (décision EB-27)
    venue_name      VARCHAR(255)     NOT NULL,
    venue_address   VARCHAR(512)     NOT NULL,
    venue_latitude  DOUBLE PRECISION NOT NULL,
    venue_longitude DOUBLE PRECISION NOT NULL,
    deleted_at      TIMESTAMPTZ,                 -- suppression logique (décision EB-31)
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_venue_position ON events (venue_latitude, venue_longitude);

-- Types de tickets d'un événement. L'id est celui de Strapi (tkt_...).
CREATE TABLE tickets (
    id        VARCHAR(64)  PRIMARY KEY,
    event_id  VARCHAR(64)  NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    name      VARCHAR(255) NOT NULL,
    price     BIGINT       NOT NULL CHECK (price >= 0),     -- en centimes
    quantity  INTEGER      NOT NULL CHECK (quantity >= 0)   -- stock total défini dans Strapi
);

CREATE INDEX idx_tickets_event_id ON tickets (event_id);

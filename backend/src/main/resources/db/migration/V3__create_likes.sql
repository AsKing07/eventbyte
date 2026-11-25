-- Likes des utilisateurs sur les événements.
-- user_id est le sub du JWT Keycloak (UserId).
-- La clé primaire (user_id, event_id) garantit qu'un utilisateur ne like qu'une fois un événement.
CREATE TABLE likes (
    user_id   VARCHAR(255) NOT NULL,
    event_id  VARCHAR(64)  NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    liked_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, event_id)
);

-- Compter les likes d'un événement (likedCount) sans parcourir toute la table.
CREATE INDEX idx_likes_event_id ON likes (event_id);

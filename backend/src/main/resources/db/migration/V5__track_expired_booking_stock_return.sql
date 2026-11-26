-- Permet de reprendre la restitution Redis si le processus s'interrompt après l'expiration.
ALTER TABLE bookings
    ADD COLUMN stock_returned BOOLEAN NOT NULL DEFAULT TRUE;

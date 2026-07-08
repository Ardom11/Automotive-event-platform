CREATE TYPE ticket_status AS ENUM ('ACTIVE', 'USED');
CREATE TYPE payment_status AS ENUM ('PENDING', 'SUCCEEDED', 'FAILED', 'EXPIRED');

CREATE TABLE ticket_payments
(
    id                BIGSERIAL PRIMARY KEY,
    stripe_payment_id TEXT           NOT NULL UNIQUE,
    user_id           BIGINT REFERENCES users (id),
    guest_name        VARCHAR(255),
    guest_surname     VARCHAR(255),
    guest_email       VARCHAR(255),
    amount_paid       NUMERIC(10, 2) NOT NULL,
    quantity          INTEGER        NOT NULL,
    status            payment_status NOT NULL,
    paid_at           TIMESTAMP,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ticket_payments_user_id ON ticket_payments (user_id);

CREATE TABLE tickets
(
    id          BIGSERIAL PRIMARY KEY,
    payment_id  BIGINT         NOT NULL REFERENCES ticket_payments (id),
    event_id    BIGINT         NOT NULL REFERENCES events (id),
    user_id     BIGINT REFERENCES users (id),
    guest_email VARCHAR(255),
    code        VARCHAR(255)   NOT NULL UNIQUE,
    status      ticket_status  NOT NULL,
    price       NUMERIC(10, 2) NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tickets_payment_id ON tickets (payment_id);
CREATE INDEX idx_tickets_event_id ON tickets (event_id);
CREATE INDEX idx_tickets_user_id ON tickets (user_id);

CREATE TABLE application_payments
(
    id                BIGSERIAL PRIMARY KEY,
    stripe_payment_id TEXT           NOT NULL UNIQUE,
    application_id    BIGINT         NOT NULL REFERENCES applications (id),
    amount_paid       NUMERIC(10, 2) NOT NULL,
    status            payment_status NOT NULL,
    paid_at           TIMESTAMP,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_application_payments_application_id ON application_payments (application_id);

ALTER TABLE ticket_payments
    ADD CONSTRAINT chk_ticket_payments_user_or_guest
        CHECK (
            (user_id IS NOT NULL AND guest_name IS NULL AND guest_surname IS NULL AND guest_email IS NULL)
                OR
            (user_id IS NULL AND guest_name IS NOT NULL AND guest_surname IS NOT NULL AND guest_email IS NOT NULL)
            );

ALTER TABLE tickets
    ADD CONSTRAINT chk_tickets_user_or_guest
        CHECK (
            (user_id IS NOT NULL AND guest_email IS NULL)
                OR
            (user_id IS NULL AND guest_email IS NOT NULL)
            );
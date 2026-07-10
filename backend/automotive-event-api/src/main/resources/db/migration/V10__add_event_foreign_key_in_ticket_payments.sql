ALTER TABLE ticket_payments
    ADD COLUMN event_id BIGINT NOT NULL REFERENCES events (id);

CREATE INDEX idx_ticket_payments_event_id
    ON ticket_payments (event_id);
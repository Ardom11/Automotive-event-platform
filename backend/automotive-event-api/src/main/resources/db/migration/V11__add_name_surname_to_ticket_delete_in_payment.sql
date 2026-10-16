ALTER TABLE ticket_payments DROP CONSTRAINT chk_ticket_payments_user_or_guest;
ALTER TABLE tickets DROP CONSTRAINT chk_tickets_user_or_guest;

CREATE TABLE ticket_payment_holders
(
    payment_id BIGINT       NOT NULL,
    position   INTEGER      NOT NULL,
    name       VARCHAR(255) NOT NULL,
    surname    VARCHAR(255) NOT NULL,
    PRIMARY KEY (payment_id, position),
    CONSTRAINT fk_ticket_payment_holders_payment
        FOREIGN KEY (payment_id) REFERENCES ticket_payments (id)
            ON DELETE CASCADE
);

ALTER TABLE tickets
    ADD COLUMN name VARCHAR(255);
ALTER TABLE tickets
    ADD COLUMN surname VARCHAR(255);

--    authenticated purchases -> copy from the linked user
UPDATE tickets t
SET name    = u.name,
    surname = u.surname FROM users u
WHERE t.user_id = u.id
  AND t.name IS NULL;

--    guest purchases -> copy from the old guest_name/guest_surname on ticket_payments
UPDATE tickets t
SET name    = tp.guest_name,
    surname = tp.guest_surname FROM ticket_payments tp
WHERE t.payment_id = tp.id
  AND t.name IS NULL;

ALTER TABLE tickets
    ALTER COLUMN name SET NOT NULL;
ALTER TABLE tickets
    ALTER COLUMN surname SET NOT NULL;

ALTER TABLE ticket_payments DROP COLUMN guest_name;
ALTER TABLE ticket_payments DROP COLUMN guest_surname;
ALTER TABLE ticket_payments DROP COLUMN quantity;
ALTER TABLE tickets DROP COLUMN guest_email;

ALTER TABLE ticket_payments
    ADD CONSTRAINT chk_ticket_payments_user_or_guest
        CHECK (
            (user_id IS NOT NULL AND guest_email IS NULL)
                OR
            (user_id IS NULL AND guest_email IS NOT NULL)
            );
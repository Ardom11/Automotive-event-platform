ALTER TABLE ticket_payments
    ALTER COLUMN stripe_payment_id DROP NOT NULL;

ALTER TABLE application_payments
    ALTER COLUMN stripe_payment_id DROP NOT NULL;
ALTER TABLE application_payments
    ADD CONSTRAINT uq_application_payments_application_id UNIQUE (application_id);
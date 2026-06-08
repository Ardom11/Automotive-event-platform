CREATE TYPE event_status AS ENUM ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED');

CREATE TABLE events
(
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(100)   NOT NULL,
    description        TEXT           NOT NULL,
    location_place     VARCHAR(255)   NOT NULL,
    location_address   VARCHAR(255)   NOT NULL,
    location_city      VARCHAR(255)   NOT NULL,
    location_country   VARCHAR(255)   NOT NULL,
    location_latitude  NUMERIC(9, 6),
    location_longitude NUMERIC(9, 6),
    date_start         TIMESTAMP      NOT NULL,
    date_end           TIMESTAMP      NOT NULL,
    tickets_capacity   INTEGER        NOT NULL,
    ticket_price       NUMERIC(10, 2) NOT NULL,
    application_fee    NUMERIC(10, 2) NOT NULL,
    status             event_status   NOT NULL DEFAULT 'DRAFT',
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
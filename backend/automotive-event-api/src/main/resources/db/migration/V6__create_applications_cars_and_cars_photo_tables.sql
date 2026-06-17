CREATE TYPE application_status AS ENUM ('DRAFT', 'PENDING', 'APPROVED_WAITING_PAYMENT', 'REJECTED', 'COMPLETED', 'EXPIRED');

CREATE TABLE applications
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT             NOT NULL REFERENCES users (id),
    event_id         BIGINT             NOT NULL REFERENCES events (id),
    status           application_status NOT NULL DEFAULT 'DRAFT',
    fee              NUMERIC(10, 2),
    rejection_reason TEXT,
    created_at       TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, event_id)
);
CREATE INDEX idx_applications_event_id ON applications (event_id);

CREATE TABLE cars
(
    id             BIGSERIAL PRIMARY KEY,
    application_id BIGINT       NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    brand          VARCHAR(255) NOT NULL,
    model          VARCHAR(255) NOT NULL,
    year           SMALLINT     NOT NULL,
    story          TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_cars_application_id ON cars (application_id);

CREATE TABLE car_photos
(
    id         BIGSERIAL PRIMARY KEY,
    car_id     BIGINT       NOT NULL REFERENCES cars (id) ON DELETE CASCADE,
    s3_key     VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_car_photos_car_id ON car_photos (car_id);
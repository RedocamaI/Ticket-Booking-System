CREATE TABLE trains (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    train_number        VARCHAR(20) NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    source              VARCHAR(100) NOT NULL,
    destination         VARCHAR(100) NOT NULL,
    total_seats         INT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

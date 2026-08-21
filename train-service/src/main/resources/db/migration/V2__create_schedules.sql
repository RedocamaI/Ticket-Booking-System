CREATE TABLE schedules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    train_id            UUID NOT NULL REFERENCES trains(id),
    travel_date         DATE NOT NULL,
    departure_time      TIME NOT NULL,
    arrival_time        TIME NOT NULL,
    price               NUMERIC(10, 2) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_train_date UNIQUE (train_id, travel_date)
);

CREATE INDEX idx_schedules_train_id ON schedules(train_id);
CREATE INDEX idx_schedules_travel_date ON schedules(travel_date);

CREATE TYPE seat_class AS ENUM ('SLEEPER', 'AC');
CREATE TYPE seat_status AS ENUM ('AVAILABLE', 'LOCKED', 'BOOKED');

CREATE TABLE seats (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    train_id            UUID NOT NULL REFERENCES trains(id),
    schedule_id         UUID NOT NULL REFERENCES schedules(id),
    seat_number         VARCHAR(10) NOT NULL,
    class               seat_class NOT NULL,
    status              seat_status NOT NULL DEFAULT 'AVAILABLE',
    version             INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_seat_schedule UNIQUE (schedule_id, seat_number)
);

CREATE INDEX idx_seats_schedule_id ON seats(schedule_id);
CREATE INDEX idx_seats_status ON seats(status);

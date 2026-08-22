CREATE TABLE seats (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    train_id            UUID NOT NULL REFERENCES trains(id),
    schedule_id         UUID NOT NULL REFERENCES schedules(id),
    seat_number         VARCHAR(10) NOT NULL,
    class               VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version             INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_seat_schedule UNIQUE (schedule_id, seat_number),
    CONSTRAINT chk_seat_class CHECK (class IN ('SLEEPER', 'AC')),
    CONSTRAINT chk_seat_status CHECK (status IN ('AVAILABLE', 'LOCKED', 'BOOKED'))
);

CREATE INDEX idx_seats_schedule_id ON seats(schedule_id);
CREATE INDEX idx_seats_status ON seats(status);

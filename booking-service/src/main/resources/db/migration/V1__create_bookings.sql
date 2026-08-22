CREATE TABLE bookings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    schedule_id     UUID NOT NULL,
    seat_id         UUID NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    booked_at       TIMESTAMP NOT NULL DEFAULT now(),
    expires_at      TIMESTAMP NOT NULL,

    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status_expires ON bookings(status, expires_at);

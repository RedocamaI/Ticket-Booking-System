-- Trains
INSERT INTO trains (id, train_number, name, source, destination, total_seats) VALUES
                                                                                  ('a1b2c3d4-0000-0000-0000-000000000001'::uuid, '12301', 'Rajdhani Express', 'Delhi', 'Mumbai', 60),
                                                                                  ('a1b2c3d4-0000-0000-0000-000000000002'::uuid, '12951', 'Shatabdi Express', 'Delhi', 'Ahmedabad', 60),
                                                                                  ('a1b2c3d4-0000-0000-0000-000000000003'::uuid, '12621', 'Tamil Nadu Express', 'Delhi', 'Chennai', 60);

-- Schedules
INSERT INTO schedules (id, train_id, travel_date, departure_time, arrival_time, price) VALUES
                                                                                           ('b1b2c3d4-0000-0000-0000-000000000001'::uuid, 'a1b2c3d4-0000-0000-0000-000000000001'::uuid, '2026-09-01', '16:00', '08:00', 1500.00),
                                                                                           ('b1b2c3d4-0000-0000-0000-000000000002'::uuid, 'a1b2c3d4-0000-0000-0000-000000000002'::uuid, '2026-09-01', '06:00', '13:00', 1200.00),
                                                                                           ('b1b2c3d4-0000-0000-0000-000000000003'::uuid, 'a1b2c3d4-0000-0000-0000-000000000003'::uuid, '2026-09-01', '22:30', '07:30', 1800.00);

-- Seats for Rajdhani Express
INSERT INTO seats (train_id, schedule_id, seat_number, class, status)
SELECT
    'a1b2c3d4-0000-0000-0000-000000000001'::uuid,
    'b1b2c3d4-0000-0000-0000-000000000001'::uuid,
    'SL-' || LPAD(gs::TEXT, 2, '0'),
    'SLEEPER'::seat_class,
    'AVAILABLE'::seat_status
FROM generate_series(1, 30) gs
UNION ALL
SELECT
    'a1b2c3d4-0000-0000-0000-000000000001'::uuid,
    'b1b2c3d4-0000-0000-0000-000000000001'::uuid,
    'AC-' || LPAD(gs::TEXT, 2, '0'),
    'AC'::seat_class,
    'AVAILABLE'::seat_status
FROM generate_series(1, 30) gs;

-- Seats for Shatabdi Express
INSERT INTO seats (train_id, schedule_id, seat_number, class, status)
SELECT
    'a1b2c3d4-0000-0000-0000-000000000002'::uuid,
    'b1b2c3d4-0000-0000-0000-000000000002'::uuid,
    'SL-' || LPAD(gs::TEXT, 2, '0'),
    'SLEEPER'::seat_class,
    'AVAILABLE'::seat_status
FROM generate_series(1, 30) gs
UNION ALL
SELECT
    'a1b2c3d4-0000-0000-0000-000000000002'::uuid,
    'b1b2c3d4-0000-0000-0000-000000000002'::uuid,
    'AC-' || LPAD(gs::TEXT, 2, '0'),
    'AC'::seat_class,
    'AVAILABLE'::seat_status
FROM generate_series(1, 30) gs;

-- Seats for Tamil Nadu Express
INSERT INTO seats (train_id, schedule_id, seat_number, class, status)
SELECT
    'a1b2c3d4-0000-0000-0000-000000000003'::uuid,
    'b1b2c3d4-0000-0000-0000-000000000003'::uuid,
    'SL-' || LPAD(gs::TEXT, 2, '0'),
    'SLEEPER'::seat_class,
    'AVAILABLE'::seat_status
FROM generate_series(1, 30) gs
UNION ALL
SELECT
    'a1b2c3d4-0000-0000-0000-000000000003'::uuid,
    'b1b2c3d4-0000-0000-0000-000000000003'::uuid,
    'AC-' || LPAD(gs::TEXT, 2, '0'),
    'AC'::seat_class,
    'AVAILABLE'::seat_status
FROM generate_series(1, 30) gs;

package com.redocmi.train_service.repository;

import com.redocmi.train_service.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByScheduleId(UUID scheduleId);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.schedule.id = :scheduleId AND s.status = 'AVAILABLE'")
    Long countAvailableSeatsByScheduleId(@Param("scheduleId") UUID scheduleId);

    @Query("SELECT s FROM Seat s WHERE s.schedule.id = :scheduleId")
    List<Seat> findByScheduleIdWithDetails(@Param("scheduleId") UUID scheduleId);

    @Query(value = """
        SELECT id FROM seats
        WHERE schedule_id = :scheduleId
        AND class = :seatClass
        AND status = 'AVAILABLE'
        LIMIT :quantity
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<UUID> findAvailableSeats(
            @Param("scheduleId") UUID scheduleId,
            @Param("seatClass") String seatClass,
            @Param("quantity") Integer quantity
    );

    List<Seat> findByScheduleIdAndSeatClass(UUID scheduleId, Seat.SeatClass seatClass);
}

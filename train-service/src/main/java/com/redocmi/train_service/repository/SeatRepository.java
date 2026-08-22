package com.redocmi.train_service.repository;

import com.redocmi.train_service.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByScheduleId(UUID scheduleId);
}

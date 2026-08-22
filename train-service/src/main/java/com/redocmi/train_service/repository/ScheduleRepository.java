package com.redocmi.train_service.repository;

import com.redocmi.train_service.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    @Query("SELECT s FROM Schedule s JOIN FETCH s.train WHERE s.id = :id")
    Optional<Schedule> findByIdWithTrain(UUID id);
}

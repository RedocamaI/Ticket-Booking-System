package com.redocmi.train_service.repository;

import com.redocmi.train_service.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    @Query("SELECT s FROM Schedule s JOIN FETCH s.train WHERE s.id = :id")
    Optional<Schedule> findByIdWithTrain(UUID id);

    @Query("""
        SELECT s FROM Schedule s
        JOIN FETCH s.train t
        WHERE LOWER(t.source) = LOWER(:source)
        AND LOWER(t.destination) = LOWER(:destination)
        AND s.travelDate = :travelDate
        """)
    List<Schedule> searchSchedules(
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("travelDate") LocalDate travelDate
    );
}

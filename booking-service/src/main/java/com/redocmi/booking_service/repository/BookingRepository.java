package com.redocmi.booking_service.repository;

import com.redocmi.booking_service.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByUserId(UUID userId, Pageable pageable);
    List<Booking> findByStatusAndExpiresAtBefore(
            Booking.BookingStatus status, LocalDateTime dateTime);
}

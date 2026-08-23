package com.redocmi.booking_service.repository;

import com.redocmi.booking_service.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUserId(UUID userId);
    List<Booking> findByStatusAndExpiresAtBefore(
            Booking.BookingStatus status, LocalDateTime dateTime);
}

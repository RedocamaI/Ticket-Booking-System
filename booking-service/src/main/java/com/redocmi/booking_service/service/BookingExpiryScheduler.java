package com.redocmi.booking_service.service;

import com.redocmi.booking_service.client.TrainServiceClient;
import com.redocmi.booking_service.entity.Booking;
import com.redocmi.booking_service.repository.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {
    private final BookingRepository bookingRepository;
    private final TrainServiceClient trainServiceClient;

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void expireStaleBookings() {
        log.info("running booking expiry scheduler at: {}", LocalDateTime.now());

        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(
                Booking.BookingStatus.PENDING,
                LocalDateTime.now()
        );

        if(expiredBookings.isEmpty()) {
            log.info("No expired bookings found.");
        }

        log.info("Found {} expired booking(s) to cancel", expiredBookings.size());
        expiredBookings.forEach(booking -> {
            try {
                booking.setStatus(Booking.BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                trainServiceClient.releaseSeat(booking.getSeatId());

                log.info("Expired booking {} cancelled and seat {} release.", booking.getId(), booking.getSeatId());
            } catch (Exception exception) {
                log.error("Failed to expire the booking {}:{}", booking.getId(), exception.getMessage());
            }
        });

        log.info("Expiry scheduler completed. Cancelled {} booking(s)", expiredBookings.size());
    }
}

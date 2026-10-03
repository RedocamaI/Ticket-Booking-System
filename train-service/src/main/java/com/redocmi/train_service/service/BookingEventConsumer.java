package com.redocmi.train_service.service;

import com.redocmi.train_service.dto.event.BookingCancelledEvent;
import com.redocmi.train_service.dto.event.BookingConfirmedEvent;
import com.redocmi.train_service.entity.Seat;
import com.redocmi.train_service.repository.SeatRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventConsumer {
    private final SeatRepository seatRepository;
    private final SeatInventoryService seatInventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "booking.confirmed", groupId = "train-service-group")
    @Transactional
    public void handleBookingConfirmed(String message) {
        try {
            BookingConfirmedEvent event = objectMapper.readValue(
                    message, BookingConfirmedEvent.class);

            log.info("Consumed booking.confirmed for booking {}", event.getBookingId());

//            Update seats status to BOOKED in db
            List<Seat> seats = seatRepository.findAllById(event.getSeatIds())
                    .stream()
                    .peek(seat -> {
//                        Idempotency check:
                        if(seat.getStatus() != Seat.SeatStatus.BOOKED) {
                            seat.setStatus(Seat.SeatStatus.BOOKED);
                        }
                    })
                    .toList();
            seatRepository.saveAll(seats);
            log.info("Seats {} set to BOOKED for booking {}", event.getSeatIds(), event.getBookingId());
        } catch (Exception exception) {
            log.info("Failed to process booking.confirmed event: {}", exception.getMessage());
        }
    }

    @KafkaListener(topics = "booking.cancelled", groupId = "train-service-group")
    @Transactional
    public void handleBookingCancelled(String message) {
        try {
            BookingCancelledEvent event = objectMapper.readValue(
                    message, BookingCancelledEvent.class);
            log.info("Consumed booking.cancelled for booking {}", event.getBookingId());

//            Update seats status to CANCELLED in db
            List<Seat> seats = seatRepository.findAllById(event.getSeatIds())
                    .stream()
                    .peek(seat -> {
                        if(seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
                            seat.setStatus(Seat.SeatStatus.AVAILABLE);
                        }
                    })
                    .toList();
            seatRepository.saveAll(seats);

//            Return seats to Redis inventory
            seatInventoryService.returnSeatsToInventory(event.getScheduleId(), event.getSeatClass(), event.getSeatIds());
            log.info("Seats {} returned to AVAILABLE for booking {}", event.getSeatIds(), event.getBookingId());
        } catch (Exception exception) {
            log.error("Failed to process booking.cancelled event: {}", exception.getMessage());
        }
    }
}

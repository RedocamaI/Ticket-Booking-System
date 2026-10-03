package com.redocmi.booking_service.service;

import com.redocmi.booking_service.dto.event.BookingCancelledEvent;
import com.redocmi.booking_service.dto.event.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String BOOKING_CONFIRMED_TOPIC = "booking.confirmed";
    private static final String BOOKING_CANCELLED_TOPIC = "booking.cancelled";

    public void publishBookingConfirmed(BookingConfirmedEvent bookingConfirmedEvent) {
        try {
            String payload = objectMapper.writeValueAsString(bookingConfirmedEvent);
            kafkaTemplate.send(BOOKING_CONFIRMED_TOPIC, bookingConfirmedEvent.getBookingId().toString(), payload);
            log.info("published booking.confirmed event for booking: {}", bookingConfirmedEvent.getBookingId());
        } catch (Exception exception) {
            log.error("Failed to publish booking.confirmed event: {}", exception.getMessage());
        }
    }

    public void publishBookingCancelled(BookingCancelledEvent bookingCancelledEvent) {
        try {
            String payload = objectMapper.writeValueAsString(bookingCancelledEvent);
            kafkaTemplate.send(BOOKING_CANCELLED_TOPIC, bookingCancelledEvent.getBookingId().toString(), payload);
            log.info("published booking.cancelled event for booking: {}", bookingCancelledEvent.getBookingId());
        } catch (Exception exception) {
            log.error("Failed to publish booking.cancelled event: {}", exception.getMessage());
        }
    }
}

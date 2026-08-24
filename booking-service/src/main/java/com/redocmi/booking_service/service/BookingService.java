package com.redocmi.booking_service.service;

import com.redocmi.booking_service.client.TrainServiceClient;
import com.redocmi.booking_service.dto.request.CreateBookingRequest;
import com.redocmi.booking_service.dto.response.BookingResponse;
import com.redocmi.booking_service.entity.Booking;
import com.redocmi.booking_service.repository.BookingRepository;
import com.redocmi.booking_service.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TrainServiceClient trainServiceClient;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, UUID userId) {
//        lock the seat in the train service first,
//        if the seat is not available trainServiceClient will throw
//        SeatNotAvailableException.
        trainServiceClient.lockSeat(request.getSeatId());

        Booking booking = Booking.builder()
                .userId(userId)
                .scheduleId(request.getScheduleId())
                .seatId(request.getSeatId())
                .status(Booking.BookingStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        return mapToBookingResponse(savedBooking);
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .scheduleId(booking.getScheduleId())
                .seatId(booking.getSeatId())
                .status(booking.getStatus().name())
                .bookedAt(booking.getBookedAt())
                .expiresAt(booking.getExpiresAt())
                .build();
    }
}

package com.redocmi.booking_service.service;

import com.redocmi.booking_service.client.TrainServiceClient;
import com.redocmi.booking_service.dto.request.CreateBookingRequest;
import com.redocmi.booking_service.dto.response.BookingResponse;
import com.redocmi.booking_service.dto.response.PageResponse;
import com.redocmi.booking_service.dto.response.PaymentResponse;
import com.redocmi.booking_service.entity.Booking;
import com.redocmi.booking_service.entity.Payment;
import com.redocmi.booking_service.exception.*;
import com.redocmi.booking_service.repository.BookingRepository;
import com.redocmi.booking_service.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TrainServiceClient trainServiceClient;

    public BookingResponse getBookingById(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking with id: " + bookingId + " does not exist."));

        if(!booking.getUserId().equals(userId)) {
            throw new UnauthorizedException(
                    "User not authorized to view this booking."
            );
        }

        return mapToBookingResponse(booking);
    }

    public PageResponse<BookingResponse> getBookingsByUser(UUID userId, int page, int size) {
        int cappedSize = Math.min(size, 10);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "bookedAt"));

        Page<Booking> bookingPage = bookingRepository.findByUserId(userId, pageable);

        List<BookingResponse> content = bookingPage.getContent()
                .stream()
                .map(this::mapToBookingResponse)
                .toList();

        return PageResponse.<BookingResponse>builder()
                .content(content)
                .page(bookingPage.getNumber())
                .size(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .last(bookingPage.isLast())
                .build();
    }

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

    public PaymentResponse cancelBooking(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking with id " + bookingId + " does not exist."
                ));

        log.info("Booking fetched successfully.");
//        verify the ownership:
        if(!booking.getUserId().equals(userId)) {
            throw new UnauthorizedException(
                    "User not authorized to cancel this booking."
            );
        }

        log.info("User authorized successfully: {}", userId);

        if(booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new BookingNotConfirmedException(
                    "Only confirmed bookings can be cancelled. Current status: " + booking.getStatus()
            );
        }

        log.info("3. booking is confirmed, need to cancel it.");

//        cancel the booking:
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        trainServiceClient.releaseSeat(booking.getSeatId());

//        Fetch real price for refund:
        BigDecimal price = trainServiceClient.getSchedulePrice(booking.getScheduleId());

//        create a refund payment record:
        Payment refundPayment = Payment.builder()
                .booking(booking)
                .amount(price)
                .status(Payment.PaymentStatus.REFUNDED)
                .gatewayRef(UUID.randomUUID().toString())
                .paidAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(refundPayment);
        log.info("Booking {} cancelled and refund created for user {}", bookingId, userId);

        return mapToPaymentResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse processPayment(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking with id: " + bookingId + " does not exist."));

        if(!booking.getUserId().equals(userId)) {
            throw new UnauthorizedException("User not authorized to pay for this booking.");
        }

        if(booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new BookingNotPendingException("Booking is not in PENDING state: " + booking.getStatus());
        }

        if(LocalDateTime.now().isAfter(booking.getExpiresAt())) {
            throw new BookingExpiredException("Booking has expired: " + bookingId);
        }

//        Fetch the real price from train service
        BigDecimal price = trainServiceClient.getSchedulePrice(booking.getScheduleId());
        log.info("Fetched price {} for schedule {}", price, booking.getScheduleId());

//        simulate 90% success rate: hardcoded for now,
//        will build with complete payment gateway in the future.
        boolean paymentSuccess = Math.random() < 0.9;
        log.info("Payment simulation result for booking {} : {}", bookingId,
                paymentSuccess ? "SUCCESS" : "FAILED");

        Payment saved;
        if(paymentSuccess) {
            // first confirm the seat, if an error occurs at least
            // we won't write to the DB.
            trainServiceClient.confirmSeat(booking.getSeatId());
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            Payment payment = Payment.builder()
                    .booking(booking)
                    .amount(price)
                    .status(Payment.PaymentStatus.SUCCESS)
                    .gatewayRef(UUID.randomUUID().toString())
                    .paidAt(LocalDateTime.now())
                    .build();

            saved = paymentRepository.save(payment);
            log.info("Payment successful for booking {} ", bookingId);
        } else {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            trainServiceClient.releaseSeat(booking.getSeatId());

            Payment payment = Payment.builder()
                    .booking(booking)
                    .amount(price)
                    .status(Payment.PaymentStatus.FAILED)
                    .gatewayRef(UUID.randomUUID().toString())
                    .paidAt(LocalDateTime.now())
                    .build();

            saved = paymentRepository.save(payment);
            log.info("Payment failed for booking: {}", bookingId);
        }

        return mapToPaymentResponse(saved);
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

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .gatewayRef(payment.getGatewayRef())
                .paidAt(payment.getPaidAt())
                .build();
    }
}

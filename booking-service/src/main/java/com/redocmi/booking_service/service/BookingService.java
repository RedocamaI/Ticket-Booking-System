package com.redocmi.booking_service.service;

import com.redocmi.booking_service.client.TrainServiceClient;
import com.redocmi.booking_service.dto.event.BookingCancelledEvent;
import com.redocmi.booking_service.dto.event.BookingConfirmedEvent;
import com.redocmi.booking_service.dto.request.CreateBookingRequest;
import com.redocmi.booking_service.dto.response.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TrainServiceClient trainServiceClient;
    private final BookingRateLimiter bookingRateLimiter;
    private final SeatLockService seatLockService;
    private final BookingEventPublisher eventPublisher;

    @Transactional
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

    public PageResponse<BookingSummaryResponse> getBookingsByUser(UUID userId, int page, int size) {
        int cappedSize = Math.min(size, 10);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "bookedAt"));

        Page<Booking> bookingPage = bookingRepository.findByUserId(userId, pageable);

        List<BookingSummaryResponse> content = bookingPage.getContent()
                .stream()
                .map(this::mapToBookingSummaryResponse)
                .toList();

        return PageResponse.<BookingSummaryResponse>builder()
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
//        Check user level rate limiting
        bookingRateLimiter.checkRateLimit(userId);

//        Acquire seats from redis: fail-closed if redis is down
        List<UUID> seatIds = seatLockService.acquireSeats(
                request.getScheduleId(), request.getSeatClass(),
                request.getQuantity(), userId);

        Booking booking = Booking.builder()
                .userId(userId)
                .scheduleId(request.getScheduleId())
                .quantity(request.getQuantity())
                .seatClass(request.getSeatClass())
                .seatIds(seatIds)
                .status(Booking.BookingStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking {} created with {} seats for user {}",
                savedBooking.getId(), seatIds.size(), userId);

        return mapToBookingResponse(savedBooking);
    }

    @Transactional
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

//        Fetch real price for refund:
//        Next issue: will store the price in booking table itself. No need to call train-service
//        through REST.
        BigDecimal price = trainServiceClient.getSchedulePrice(booking.getScheduleId());

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
//        create a refund payment record:
        Payment refundPayment = Payment.builder()
                .booking(booking)
                .amount(price)
                .status(Payment.PaymentStatus.REFUNDED)
                .gatewayRef(UUID.randomUUID().toString())
                .paidAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(refundPayment);

        try {
            eventPublisher.publishBookingCancelled(BookingCancelledEvent.builder()
                    .bookingId(booking.getId())
                    .userID(booking.getUserId())
                    .scheduleId(booking.getScheduleId())
                    .seatClass(booking.getSeatClass())
                    .seatIds(booking.getSeatIds())
                    .build());
            seatLockService.releaseSeats(booking.getScheduleId(), booking.getSeatClass(), booking.getSeatIds());
        } catch (Exception exception) {
            log.error("Kafka publish failed for cancelled booking {}. " +
                    "Manual intervention required. SeatIds: {}", booking.getId(), booking.getSeatIds());
            // TODO [Phase 3]: Transactional Outbox Pattern eliminates this scenario
        }
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
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            Payment payment = Payment.builder()
                    .booking(booking)
                    .amount(price)
                    .status(Payment.PaymentStatus.SUCCESS)
                    .gatewayRef(UUID.randomUUID().toString())
                    .paidAt(LocalDateTime.now())
                    .build();

            try {
                eventPublisher.publishBookingConfirmed(BookingConfirmedEvent.builder()
                        .bookingId(booking.getId())
                        .userId(booking.getUserId())
                        .scheduleId(booking.getScheduleId())
                        .seatClass(booking.getSeatClass())
                        .seatIds(booking.getSeatIds())
                        .build());

                // Only delete Redis locks after successful publish
                seatLockService.confirmSeats(booking.getSeatIds());
            } catch (Exception exception) {
                log.error("Kafka publish failed for booking {}:{}", booking.getId(), exception.getMessage());
                // Don't throw — booking is confirmed, payment taken
                // Seats will remain in AVAILABLE state in DB until manual fix
                // TODO [Phase 3]: Transactional Outbox Pattern eliminates this scenario
            }

            saved = paymentRepository.save(payment);
            log.info("Payment successful for booking {} ", bookingId);
        } else {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);
//            the below call will be replaced by kafka event in the future updates:
            trainServiceClient.releaseSeats(booking.getSeatIds());

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
                .seatIds(booking.getSeatIds())
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

    private BookingSummaryResponse mapToBookingSummaryResponse(Booking booking) {
        return new BookingSummaryResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getScheduleId(),
                booking.getQuantity(),
                booking.getStatus().name(),
                booking.getBookedAt(),
                booking.getExpiresAt()
        );
    }
}

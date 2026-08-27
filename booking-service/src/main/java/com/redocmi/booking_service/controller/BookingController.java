package com.redocmi.booking_service.controller;

import com.redocmi.booking_service.dto.request.CreateBookingRequest;
import com.redocmi.booking_service.dto.response.ApiResponse;
import com.redocmi.booking_service.dto.response.BookingResponse;
import com.redocmi.booking_service.dto.response.PaymentResponse;
import com.redocmi.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/bookings/")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings(@RequestHeader("X-User-Id") UUID userId) {
        List<BookingResponse> bookings = bookingService.getBookingsByUser(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Bookings fetched successfully", bookings));
    }

    @GetMapping("{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable UUID bookingId,
                                                                   @RequestHeader("X-User-Id") UUID userId) {
        BookingResponse response = bookingService.getBookingById(bookingId, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Booking " + response.getId() + " fetched successfully.", response));
    }

    @PatchMapping("{bookingId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelBooking(@PathVariable UUID bookingId,
                                                                      @RequestHeader("X-User-Id") UUID userId) {
        log.info("0. initiating cancel booking.");
        PaymentResponse refundPayment = bookingService.cancelBooking(bookingId, userId);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.success("Booking cancelled and refund initiated", refundPayment));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request, @RequestHeader("X-User-Id") UUID userId) {
        BookingResponse response = bookingService.createBooking(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking: " + response.getId() + " created successfully!", response));
    }

    @PostMapping("{bookingId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId) {
        PaymentResponse paymentResponse = bookingService.processPayment(bookingId, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully", paymentResponse));
    }
}

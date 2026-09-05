package com.redocmi.booking_service.controller;

import com.redocmi.booking_service.dto.request.CreateBookingRequest;
import com.redocmi.booking_service.dto.response.ApiResponse;
import com.redocmi.booking_service.dto.response.BookingResponse;
import com.redocmi.booking_service.dto.response.PaymentResponse;
import com.redocmi.booking_service.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Bookings", description = "Booking lifecycle management")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Get all bookings for the current user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings(@RequestHeader("X-User-Id") UUID userId) {
        List<BookingResponse> bookings = bookingService.getBookingsByUser(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Bookings fetched successfully", bookings));
    }

    @Operation(summary = "Get booking by ID")
    @GetMapping("{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable UUID bookingId,
                                                                   @RequestHeader("X-User-Id") UUID userId) {
        BookingResponse response = bookingService.getBookingById(bookingId, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Booking " + response.getId() + " fetched successfully.", response));
    }

    @Operation(summary = "Cancel a confirmed booking and initiate a refund")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                description = "Booking cancelled and refund initiated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                description = "Booking not CONFIRMED")
    })
    @PatchMapping("{bookingId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelBooking(@PathVariable UUID bookingId,
                                                                      @RequestHeader("X-User-Id") UUID userId) {
        log.info("Initiating cancel booking.");
        PaymentResponse refundPayment = bookingService.cancelBooking(bookingId, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Booking cancelled and refund initiated", refundPayment));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Booking creation successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "423", description = "Seat is Locked or already Booked")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request, @RequestHeader("X-User-Id") UUID userId) {
        BookingResponse response = bookingService.createBooking(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking: " + response.getId() + " created successfully!", response));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Payment processed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Booking is not in PENDING state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "410", description = "Booking has expired")
    })
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

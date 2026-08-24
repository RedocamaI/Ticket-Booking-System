package com.redocmi.booking_service.controller;

import com.redocmi.booking_service.dto.request.CreateBookingRequest;
import com.redocmi.booking_service.dto.response.ApiResponse;
import com.redocmi.booking_service.dto.response.BookingResponse;
import com.redocmi.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request, @RequestHeader("X-User-Id") UUID userId) {
        BookingResponse response = bookingService.createBooking(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking: " + response.getId() + " created successfully!", response));
    }
}

package com.redocmi.train_service.controller;

import com.redocmi.train_service.dto.request.BatchSeatRequest;
import com.redocmi.train_service.dto.request.ReturnSeatsRequest;
import com.redocmi.train_service.dto.request.SeatIdsRequest;
import com.redocmi.train_service.dto.response.ApiResponse;
import com.redocmi.train_service.dto.response.SeatResponse;
import com.redocmi.train_service.entity.Seat;
import com.redocmi.train_service.service.TrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/")
@Tag(name = "Seats", description = "Seats availability")
public class SeatController {
    private final TrainService trainService;

    @Operation(summary = "Get all seats for a schedule")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Seats fetched successfully")
    @GetMapping("/schedules/{scheduleId}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsBySchedule(@PathVariable UUID scheduleId) {
        List<SeatResponse> seats = trainService.getSeatsByScheduleId(scheduleId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("seats fetched successfully", seats));
    }

    @Operation(summary = "Find multiple available seats atomically - internal use only")
    @PatchMapping("/internal/seats/find-batch")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> findSeats(@RequestBody BatchSeatRequest request) {
        log.info("SeatController - scheduleId: {}", request.getScheduleId());
        List<SeatResponse> seats = trainService.lockSeats(
                request.getScheduleId(),
                request.getSeatClass(),
                request.getQuantity());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Available seats found successfully: ", seats));
    }

    @Operation(summary = "Confirm multiple seats - internal use only")
    @PatchMapping("/internal/seats/confirm-batch")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> confirmSeats(@RequestBody SeatIdsRequest seatIdsRequest) {
        List<SeatResponse> confirmedSeats = trainService.confirmSeats(seatIdsRequest.getSeatIds());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("seats confirmed successfully: ", confirmedSeats));
    }

    @Operation(summary = "Release multiple seats - internal use only")
    @PatchMapping("/internal/seats/release-batch")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> releaseSeat(@RequestBody SeatIdsRequest seatIdsRequest) {
        log.info("SeatIdsRequest: {}", seatIdsRequest);
        List<SeatResponse> releasedSeats = trainService.releaseSeats(seatIdsRequest.getSeatIds());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("seats released successfully: ", releasedSeats));
    }

    @Operation(summary = "Return multiple seats upon cancellation - internal use only")
    @PatchMapping("/internal/seats/return-batch")
    public ResponseEntity<ApiResponse<Void>> returnSeats(
            @RequestBody ReturnSeatsRequest request) {
        trainService.returnSeats(
                request.getScheduleId(),
                request.getSeatClass(),
                request.getSeatIds());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Seats returned to inventory", null));
    }

    @GetMapping("/internal/schedules/{scheduleId}/price")
    public ResponseEntity<ApiResponse<BigDecimal>> getSchedulePrice(@PathVariable UUID scheduleId) {
        BigDecimal price = trainService.getSchedulePrice(scheduleId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Price fetched successfully", price));
    }
}

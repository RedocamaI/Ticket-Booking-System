package com.redocmi.train_service.controller;

import com.redocmi.train_service.dto.response.ApiResponse;
import com.redocmi.train_service.dto.response.SeatResponse;
import com.redocmi.train_service.service.TrainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/")
public class SeatController {
    private final TrainService trainService;

    @GetMapping("/schedules/{scheduleId}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsBySchedule(@PathVariable UUID scheduleId) {
        List<SeatResponse> seats = trainService.getSeatsByScheduleId(scheduleId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("seats fetched successfully", seats));
    }

    @PatchMapping("/internal/seats/{seatId}/lock")
    public ResponseEntity<ApiResponse<SeatResponse>> lockSeat(@PathVariable UUID seatId) {
        SeatResponse seatResponse = trainService.lockSeat(seatId);

//        we must return OK here: specifically a 2XX status since this will be handled to
//        confirm if a seat was locked for that specific time frame by us.
//        earlier we were using 423 which means the resource we are trying to update is LOCKED
//        but, we just locked it ourselves in the current request; so 423 seems paradoxical!
//        one moment we were trying to lock it then we returned 423 to indicate that we were
//        able to successfully lock but 423 means it was already locked for us.
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("seat " + seatId + " locked successfully", seatResponse));
    }

    @PatchMapping("/internal/seats/{seatId}/confirm")
    public ResponseEntity<ApiResponse<SeatResponse>> confirmSeat(@PathVariable UUID seatId) {
        SeatResponse seatResponse = trainService.confirmSeat(seatId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("seat " + seatResponse.getSeatNumber() + " confirmed", seatResponse));
    }

    @PatchMapping("/internal/seats/{seatId}/release")
    public ResponseEntity<ApiResponse<SeatResponse>> releaseSeat(@PathVariable UUID seatId) {
        SeatResponse seatResponse = trainService.releaseSeat(seatId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("seat " + seatResponse.getSeatNumber() + " released", seatResponse));
    }
}

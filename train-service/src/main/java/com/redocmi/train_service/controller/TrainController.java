package com.redocmi.train_service.controller;

import com.redocmi.train_service.dto.request.CreateScheduleRequest;
import com.redocmi.train_service.dto.request.CreateTrainRequest;
import com.redocmi.train_service.dto.response.ApiResponse;
import com.redocmi.train_service.dto.response.ScheduleResponse;
import com.redocmi.train_service.dto.response.TrainResponse;
import com.redocmi.train_service.dto.response.TrainSearchResponse;
import com.redocmi.train_service.service.TrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/")
public class TrainController {
    private final TrainService trainService;

    // admin endpoints:
    @PostMapping("/admin/create-train")
    public ResponseEntity<ApiResponse<TrainResponse>> createTrain(
            @Valid @RequestBody CreateTrainRequest request) {
        TrainResponse trainResponse = trainService.createTrain(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Train created successfully", trainResponse));
    }

    @PostMapping("/admin/create-schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @Valid @RequestBody CreateScheduleRequest request) {
        ScheduleResponse scheduleResponse = trainService.createSchedule(request);

        String successMessage = "Schedule for train: " + request.getTrainId() + " created successfully";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(successMessage, scheduleResponse));
    }

    // Public endpoints:
    @GetMapping("/trains/{id}")
    public ResponseEntity<ApiResponse<TrainResponse>> getTrainById(@PathVariable UUID id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Train fetched successfully", trainService.getTrainById(id)));
    }

    @GetMapping("/trains/all-trains")
    public ResponseEntity<ApiResponse<List<TrainResponse>>> getAllTrains() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("All trains fetched successfully", trainService.getAllTrains()));
    }

    @GetMapping("/schedules/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getScheduleById(@PathVariable UUID id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Schedule fetched successfully", trainService.getScheduleById(id)));
    }

    @GetMapping("/trains/search")
    public ResponseEntity<ApiResponse<List<TrainSearchResponse>>> searchTrains(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date){
        List<TrainSearchResponse> results = trainService.searchTrains(source, destination, date);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Trains fetched successfully", results));
    }
}

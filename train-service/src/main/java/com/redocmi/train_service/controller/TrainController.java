package com.redocmi.train_service.controller;

import com.redocmi.train_service.dto.request.CreateScheduleRequest;
import com.redocmi.train_service.dto.request.CreateTrainRequest;
import com.redocmi.train_service.dto.response.ApiResponse;
import com.redocmi.train_service.dto.response.ScheduleResponse;
import com.redocmi.train_service.dto.response.TrainResponse;
import com.redocmi.train_service.dto.response.TrainSearchResponse;
import com.redocmi.train_service.service.TrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Trains", description = "Train and schedule management")
public class TrainController {
    private final TrainService trainService;

    // admin endpoints:
    @Operation(summary = "create a new train(admin only")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Train created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Train number already exists")
    })
    @PostMapping("/admin/create-train")
    public ResponseEntity<ApiResponse<TrainResponse>> createTrain(
            @Valid @RequestBody CreateTrainRequest request) {
        TrainResponse trainResponse = trainService.createTrain(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Train created successfully", trainResponse));
    }

    @Operation(summary = "create a schedule and auto-generate seats (admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Schedule created with seats"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Train not found")
    })
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
    @Operation(summary = "Get train by ID")
    @GetMapping("/trains/{id}")
    public ResponseEntity<ApiResponse<TrainResponse>> getTrainById(@PathVariable UUID id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Train fetched successfully", trainService.getTrainById(id)));
    }

    @Operation(summary = "Get all trains")
    @GetMapping("/trains/all-trains")
    public ResponseEntity<ApiResponse<List<TrainResponse>>> getAllTrains() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("All trains fetched successfully", trainService.getAllTrains()));
    }

    @Operation(summary = "Get schedule by ID")
    @GetMapping("/schedules/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getScheduleById(@PathVariable UUID id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Schedule fetched successfully", trainService.getScheduleById(id)));
    }

    @Operation(summary = "Search trains by source, destination and date")
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

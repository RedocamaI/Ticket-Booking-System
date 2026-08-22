package com.redocmi.train_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainSearchResponse {
    private UUID scheduleId;
    private UUID trainId;
    private String trainNumber;
    private String trainName;
    private String source;
    private String destination;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private BigDecimal price;
    private Long availableSeats;
}

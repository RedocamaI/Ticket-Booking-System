package com.redocmi.train_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class CreateScheduleRequest {
    @NotNull(message = "Train ID is required")
    private UUID trainId;

    @NotNull(message = "travel date is required")
    private LocalDate travelDate;

    @NotNull(message = "departure time is required")
    private LocalTime departureTime;

    @NotNull(message = "arrival time is required")
    private LocalTime arrivalTime;

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    private BigDecimal price;
}

package com.redocmi.booking_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateBookingRequest {
    @NotNull(message = "schedule ID is required")
    private UUID scheduleId;

    @NotBlank(message = "Seat class is required")
    private String seatClass;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 5, message = "Maximum 5 seats allowed per booking")
    private Integer quantity;
}

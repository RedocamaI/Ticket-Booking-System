package com.redocmi.booking_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateBookingRequest {
    @NotNull(message = "schedule ID is required")
    private UUID scheduleId;

    @NotNull(message = "seat ID is required")
    private UUID seatId;
}

package com.redocmi.booking_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID id;
    private UUID userId;
    private UUID scheduleId;
    private UUID seatId;
    private String status;
    private LocalDateTime bookedAt;
    private LocalDateTime expiresAt;
}

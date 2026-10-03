package com.redocmi.train_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent {
    private UUID bookingId;
    private UUID userId;
    private UUID scheduleId;
    private String seatClass;
    private List<UUID> seatIds;
}

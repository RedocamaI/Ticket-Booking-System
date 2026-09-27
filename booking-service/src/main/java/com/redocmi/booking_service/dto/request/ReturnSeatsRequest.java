package com.redocmi.booking_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnSeatsRequest {
    private UUID scheduleId;
    private String seatClass;
    private List<UUID> seatIds;
}

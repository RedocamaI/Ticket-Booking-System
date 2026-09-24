package com.redocmi.booking_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchSeatRequest {
    private UUID scheduleId;
    private String seatClass;
    private int quantity;
}

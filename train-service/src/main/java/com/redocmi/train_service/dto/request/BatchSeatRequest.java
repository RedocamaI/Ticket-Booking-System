package com.redocmi.train_service.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class BatchSeatRequest {
    private UUID scheduleId;
    private String seatClass;
    private Integer quantity;
}

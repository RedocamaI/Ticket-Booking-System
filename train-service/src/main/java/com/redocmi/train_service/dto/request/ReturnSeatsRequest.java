package com.redocmi.train_service.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReturnSeatsRequest {
    private UUID scheduleId;
    private String seatClass;
    private List<UUID> seatIds;
}

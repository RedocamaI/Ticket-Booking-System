package com.redocmi.train_service.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SeatIdsRequest {
    private List<UUID> seatIds;
}

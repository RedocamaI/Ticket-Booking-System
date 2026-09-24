package com.redocmi.booking_service.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class SeatResponse {
    private UUID id;
    private String seatNumber;
    private String status;
}

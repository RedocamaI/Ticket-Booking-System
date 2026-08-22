package com.redocmi.train_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainResponse {
    private UUID id;
    private String trainNumber;
    private String name;
    private String source;
    private String destination;
    private int totalSeats;
}

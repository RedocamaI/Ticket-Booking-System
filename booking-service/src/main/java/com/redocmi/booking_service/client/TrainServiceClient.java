package com.redocmi.booking_service.client;

import com.redocmi.booking_service.dto.request.BatchSeatRequest;
import com.redocmi.booking_service.dto.request.ReturnSeatsRequest;
import com.redocmi.booking_service.dto.request.SeatIdsRequest;
import com.redocmi.booking_service.dto.response.SeatResponse;
import com.redocmi.booking_service.dto.response.ApiResponse;
import com.redocmi.booking_service.exception.SeatNotAvailableException;
import com.redocmi.booking_service.exception.TrainServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class TrainServiceClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TrainServiceClient(@Value("${train.service.base-url}") String baseUrl,
                              ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    public void confirmSeats(List<UUID> seatIds) {
        log.info("Confirming {} seats", seatIds.size());
        restClient.patch()
                .uri("/api/internal/seats/confirm-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SeatIdsRequest(seatIds))
                .retrieve()
                .onStatus(HttpStatusCode::isError, ((request, response) -> {
                    throw new TrainServiceException(
                            "Failed to confirm seats - " + response.getStatusCode());
                }))
                .toBodilessEntity();

        log.info("Seats confirmed successfully");
    }

    public void releaseSeats(List<UUID> seatIds) {
        log.info("Releasing {} seats", seatIds.size());
        restClient.patch()
                .uri("/api/internal/seats/release-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SeatIdsRequest(seatIds))
                .retrieve()
                .onStatus(HttpStatusCode::isError, ((request, response) -> {
                    throw new TrainServiceException(
                            "Failed to release seats");
                }))
                .toBodilessEntity();

        log.info("Seats released successfully");
    }

    public void returnSeats(UUID scheduleId, String seatClass, List<UUID> seatIds) {
        log.info("Returning {} seats to inventory for schedule {}",
                seatIds.size(), scheduleId);
        restClient.patch()
                .uri("/api/internal/seats/return-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReturnSeatsRequest(scheduleId, seatClass, seatIds))
                .retrieve()
                .onStatus(HttpStatusCode::isError, ((request, response) -> {
                    throw new TrainServiceException("Failed to return seats to inventory.");
                }))
                .toBodilessEntity();
    }

    public BigDecimal getSchedulePrice(UUID scheduleId) {
        log.info("Fetching price for schedule: {}", scheduleId);
        ApiResponse<BigDecimal> response = restClient.get()
                .uri("/api/internal/schedules/{scheduleId}/price", scheduleId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ((request, res) -> {
                    log.error("Error fetching schedule price: {}", res.getStatusCode().value());
                    throw new TrainServiceException("Failed to fetch schedule price: " + scheduleId);
                }))
                .body(new ParameterizedTypeReference<ApiResponse<BigDecimal>>() {});

        return response != null ? response.getData() : BigDecimal.ZERO;
    }

    private String extractMessage(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            return json.path("message").asString();
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }
}

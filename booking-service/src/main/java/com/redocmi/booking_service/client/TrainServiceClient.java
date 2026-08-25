package com.redocmi.booking_service.client;

import com.redocmi.booking_service.exception.SeatNotAvailableException;
import com.redocmi.booking_service.exception.TrainServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Component
public class TrainServiceClient {
    private final RestClient restClient;

    public TrainServiceClient(@Value("${train.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void lockSeat(UUID seatId) {
        restClient.patch()
                .uri("/api/internal/seats/{seatId}/lock", seatId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    int status = response.getStatusCode().value();
                    String body = new String(response.getBody().readAllBytes());
                    String message;

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode json = mapper.readTree(body);
                        message = json.path("message").asString("Train service error");
                    } catch (Exception exception) {
                        message = "Train service error: " + status;
                    }

                    if(status == 404) {
                        throw new SeatNotAvailableException(message);
                    }

                    log.error("Error from train-service: status={}, body={}", status, message);
                    throw new TrainServiceException(message);
                })
                .toBodilessEntity();
        log.info("Seat locked successfully: {}", seatId);
    }

    public void confirmSeat(UUID seatId) {
        log.info("Confirming seat: {}", seatId);
        restClient.patch()
                .uri("/api/internal/seats/{seatId}/confirm", seatId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, ((request, response) -> {
                    throw new TrainServiceException(
                            "Failed to confirm seat: " + response.getStatusCode());
                }))
                .onStatus(HttpStatusCode::is5xxServerError, ((request, response) -> {
                    throw new TrainServiceException(
                            "Train service error while confirming the seat: " + seatId);
                }))
                .toBodilessEntity();

        log.info("Seat confirmed successfully: {}", seatId);
    }

    public void releaseSeat(UUID seatId) {
        log.info("Releasing seat: {}", seatId);
        restClient.patch()
                .uri("/internal/seats/{seatId}/release", seatId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, ((request, response) -> {
                    throw new TrainServiceException(
                            "Failed to release seat: " + response.getStatusCode());
                }))
                .onStatus(HttpStatusCode::is5xxServerError, ((request, response) -> {
                    throw new TrainServiceException(
                            "Train service error while releasing the seat: " + seatId);
                }))
                .toBodilessEntity();

        log.info("Seat released successfully: {}", seatId);
    }
}

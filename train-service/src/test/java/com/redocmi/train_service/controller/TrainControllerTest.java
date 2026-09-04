package com.redocmi.train_service.controller;

import com.redocmi.train_service.dto.request.CreateTrainRequest;
import com.redocmi.train_service.dto.response.TrainResponse;
import com.redocmi.train_service.exception.DuplicateTrainException;
import com.redocmi.train_service.exception.ResourceNotFoundException;
import com.redocmi.train_service.service.TrainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrainController.class)
public class TrainControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainService trainService;

    @Test
    void createTrain_shouldSucceed_whenValidRequest() throws Exception {
        TrainResponse response = TrainResponse.builder()
                .id(UUID.randomUUID())
                .trainNumber("12301")
                .name("Rajdhani Express")
                .source("Delhi")
                .destination("Mumbai")
                .totalSeats(60)
                .build();

        when(trainService.createTrain(any(CreateTrainRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/create-train")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "trainNumber": "12301",
                            "name": "Rajdhani Express",
                            "source": "Delhi",
                            "destination": "Mumbai",
                            "totalSeats": 60
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trainNumber").value("12301"));
    }

    @Test
    void createTrain_shouldFail_whenDuplicateTrainNumber() throws Exception {
        when(trainService.createTrain(any(CreateTrainRequest.class)))
                .thenThrow(new DuplicateTrainException("Train already exists"));

        mockMvc.perform(post("/api/admin/create-train")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "trainNumber": "12301",
                            "name": "Rajdhani Express",
                            "source": "Delhi",
                            "destination": "Mumbai",
                            "totalSeats": 60
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getTrainById_shouldSucceed_whenTrainExists() throws Exception {
        UUID trainId = UUID.randomUUID();
        TrainResponse trainResponse = TrainResponse.builder()
                .id(trainId)
                .trainNumber("12301")
                .name("Rajdhani Express")
                .source("Delhi")
                .destination("Mumbai")
                .totalSeats(60)
                .build();

        when(trainService.getTrainById(trainId)).thenReturn(trainResponse);

        mockMvc.perform(get("/api/trains/{id}", trainId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trainNumber").value("12301"));
    }

    @Test
    void getTrainById_shouldFail_whenTrainIsNotFound() throws Exception{
        UUID trainId = UUID.randomUUID();
        when(trainService.getTrainById(trainId)).thenThrow(new ResourceNotFoundException("Train not found"));

        mockMvc.perform(get("/api/trains/{id}", trainId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getAllTrains_shouldReturnList() throws Exception {
        List<TrainResponse> mockResponse = List.of(
                TrainResponse.builder()
                        .id(UUID.randomUUID())
                        .trainNumber("12301")
                        .name("Rajdhani Express")
                        .source("Delhi")
                        .destination("Mumbai")
                        .totalSeats(60)
                        .build()
        );

        when(trainService.getAllTrains()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/trains/all-trains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void createTrain_shouldFail_whenMissingFields() throws Exception {
        mockMvc.perform(post("/api/admin/create-train")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "trainNumber": "12301"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

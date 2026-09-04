package com.redocmi.train_service.service;

import com.redocmi.train_service.dto.request.CreateTrainRequest;
import com.redocmi.train_service.dto.response.TrainResponse;
import com.redocmi.train_service.entity.Train;
import com.redocmi.train_service.exception.DuplicateTrainException;
import com.redocmi.train_service.exception.ResourceNotFoundException;
import com.redocmi.train_service.repository.TrainRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrainServiceTest {

    @Mock
    private TrainRepository trainRepository;

    @InjectMocks
    private TrainService trainService;

    @Test
    void createTrain_shouldSucceed_whenValidRequest() {
        CreateTrainRequest request = new CreateTrainRequest();
        request.setTrainNumber("12301");
        request.setName("Rajdhani Express");
        request.setSource("Delhi");
        request.setDestination("Mumbai");
        request.setTotalSeats(60);

        Train savedTrain = Train.builder()
                .id(UUID.randomUUID())
                .trainNumber("12301")
                .name("Rajdhani Express")
                .source("Delhi")
                .destination("Mumbai")
                .totalSeats(60)
                .build();

        when(trainRepository.existsByTrainNumber("12301")).thenReturn(false);
        when(trainRepository.save(any(Train.class))).thenReturn(savedTrain);

        TrainResponse response = trainService.createTrain(request);

        assertThat(response.getTrainNumber()).isEqualTo("12301");
        assertThat(response.getName()).isEqualTo("Rajdhani Express");
        verify(trainRepository).save(any(Train.class));
    }

    @Test
    void createTrain_shouldThrow_whenDuplicateTrainNumber() {
        CreateTrainRequest request = new CreateTrainRequest();
        request.setTrainNumber("12301");
        request.setName("Rajdhani Express");
        request.setSource("Delhi");
        request.setDestination("Mumbai");
        request.setTotalSeats(60);

        when(trainRepository.existsByTrainNumber("12301")).thenReturn(true);

        assertThatThrownBy(() -> trainService.createTrain(request))
                .isInstanceOf(DuplicateTrainException.class);
        verify(trainRepository, never()).save(any());
    }

    @Test
    void getTrainById_shouldSucceed_whenTrainExists() {
        UUID trainId = UUID.randomUUID();
        Train train = Train.builder()
                .id(trainId)
                .trainNumber("12301")
                .name("Rajdhani Express")
                .source("Delhi")
                .destination("Mumbai")
                .totalSeats(60)
                .build();

        when(trainRepository.findById(trainId)).thenReturn(Optional.of(train));

        TrainResponse response = trainService.getTrainById(trainId);

        assertThat(response.getId()).isEqualTo(trainId);
        assertThat(response.getTrainNumber()).isEqualTo("12301");
    }

    @Test
    void getTrainById_shouldFail_whenTrainNotFound() {
        UUID trainId = UUID.randomUUID();
        when(trainRepository.findById(trainId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainService.getTrainById(trainId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

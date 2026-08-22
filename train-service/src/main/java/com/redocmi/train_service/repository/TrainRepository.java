package com.redocmi.train_service.repository;

import com.redocmi.train_service.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrainRepository extends JpaRepository<Train, UUID> {
    boolean existsByTrainNumber(String trainNumber);
}

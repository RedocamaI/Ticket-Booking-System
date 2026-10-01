package com.redocmi.train_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatInventoryService {
    private final StringRedisTemplate redisTemplate;
    private static final String SEAT_INVENTORY_PREFIX = "seats:available:";

    public String buildKey(UUID scheduleId, String seatClass) {
        return SEAT_INVENTORY_PREFIX + scheduleId + ":" + seatClass;
    }

    public void populateSeatInventory(
            UUID scheduleId,
            List<UUID> sleeperSeatIds,
            List<UUID> acSeatIds,
            LocalDate travelDate,
            LocalTime departureTime) {
//        Calculate TTL - expires at departure time on travel date
        LocalDateTime expiry = LocalDateTime.of(travelDate, departureTime);
        Duration ttl = Duration.between(LocalDateTime.now(), expiry);

        if(ttl.isNegative() || ttl.isZero()) {
            log.warn("Schedule: {} expired, skipping inventory population.", scheduleId);
            return;
        }

//        Push sleeper seats:
        String sleeperKey = buildKey(scheduleId, "SLEEPER");
        String[] sleeperIds = sleeperSeatIds.stream()
                .map(UUID::toString)
                .toArray(String[]::new);

        redisTemplate.opsForSet().add(sleeperKey, sleeperIds);
        redisTemplate.expire(sleeperKey, ttl);
        log.info("Populated {} SLEEPER seats for schedule {}", sleeperIds.length, scheduleId);

//        Push ac seats:
        String acKey = buildKey(scheduleId, "AC");
        String[] acIds = acSeatIds.stream()
                .map(UUID::toString)
                .toArray(String[]::new);

        redisTemplate.opsForSet().add(acKey, acIds);
        redisTemplate.expire(acKey, ttl);
        log.info("Populated {} AC seats for schedule {}", acIds.length, scheduleId);
    }

    public void returnSeatsToInventory(UUID scheduleId, String seatClass, List<UUID> seatIds) {
        String key = buildKey(scheduleId, seatClass);
        String[] ids = seatIds.stream()
                .map(UUID::toString)
                .toArray(String[]::new);

        redisTemplate.opsForSet().add(key, ids);
        log.info("Returned {} seats to inventory for schedule {}", ids.length, scheduleId);
    }

    public Long getAvailableCount(UUID scheduleId, String seatClass) {
        String key = buildKey(scheduleId, seatClass);
        Long availableCount = redisTemplate.opsForSet().size(key);

        return availableCount != null ? availableCount : 0;
    }
}

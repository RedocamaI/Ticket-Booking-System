package com.redocmi.booking_service.service;

import com.redocmi.booking_service.exception.BookingUnavailableException;
import com.redocmi.booking_service.exception.SeatNotAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {
    private final StringRedisTemplate redisTemplate;

    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String SEAT_INVENTORY_PREFIX = "seats:available:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    public List<UUID> acquireSeats(UUID scheduleId, String seatClass,
                                   Integer quantity, UUID userId) {
        String inventoryKey = SEAT_INVENTORY_PREFIX + scheduleId + ":" + seatClass;

        try {
//            check for enough seats:
            Long available = redisTemplate.opsForSet().size(inventoryKey);
            if(available == null || available < quantity) {
                throw new SeatNotAvailableException("Not enough available seats." +
                        " Requested: " + quantity + ", Available: " + available);
            }

//            Atomically pop N seats from the inventory:
            List<UUID> acquiredSeats = new ArrayList<>();
            for(int i=0;i<quantity;i++) {
                String seatId = redisTemplate.opsForSet().pop(inventoryKey);
                if(seatId == null) {
//                    Race condition - return acquired seats to memory
                    returnSeatsToInventory(scheduleId, seatClass, acquiredSeats);
                    throw new SeatNotAvailableException(
                            "Not enough seats available after concurrent request.");
                }

                acquiredSeats.add(UUID.fromString(seatId));
            }

//            Write TTL lock for each acquired seat:
            for(UUID seatId : acquiredSeats) {
                String lockKey = SEAT_LOCK_PREFIX + seatId;
                redisTemplate.opsForValue()
                        .set(lockKey, userId.toString(), LOCK_TTL);
            }

            log.info("Acquired {} {} seats for schedule {}, user {}",
                    acquiredSeats.size(), seatClass, scheduleId, userId);

            return acquiredSeats;
        } catch (SeatNotAvailableException exception) {
            throw exception;
        } catch (Exception exception) {
            log.info("Redis error acquiring seats: {}", exception.getMessage());

            throw new BookingUnavailableException(
                    "Booking service unavailable. Please try again.");
        }
    }

    public void releaseSeats(UUID scheduleId, String seatClass, List<UUID> seatIds) {
//        Delete lock keys:
        List<String> lockKeys = seatIds.stream()
                .map(id -> SEAT_LOCK_PREFIX + id)
                .toList();
        redisTemplate.delete(lockKeys);

//        Return seats to inventory
        returnSeatsToInventory(scheduleId, seatClass, seatIds);
        log.info("Release {} seats back to inventory.", seatIds.size());
    }

    public void confirmSeats(List<UUID> seatIds) {
//        Delete the lock keys - seats stay out of inventory permanently
        List<String> lockKeys = seatIds.stream()
                .map(id -> SEAT_LOCK_PREFIX + id)
                .toList();

        redisTemplate.delete(lockKeys);
        log.info("Confirmed {} seats and removed from redis.", seatIds.size());
    }

    private void returnSeatsToInventory(
            UUID scheduleId, String seatClass, List<UUID> seatIds) {
        if(seatIds.isEmpty())
            return;

        String inventoryKey = SEAT_INVENTORY_PREFIX + scheduleId + ":" + seatClass;
        String[] ids = seatIds.stream()
                .map(UUID::toString)
                .toArray(String[]::new);

        redisTemplate.opsForSet().add(inventoryKey, ids);
    }
}

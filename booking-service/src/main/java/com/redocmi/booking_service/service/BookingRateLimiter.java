package com.redocmi.booking_service.service;

import com.redocmi.booking_service.exception.TooManyBookingRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class BookingRateLimiter {
    private static final Integer MAX_BOOKINGS_PER_DAY = 6;
    private static final Long WINDOW_MS = 86_400_000L;

    private final ConcurrentHashMap<UUID, Long[]> userRequestCounts
            = new ConcurrentHashMap<>();

    public void checkRateLimit(UUID userId) {
        Long now = Instant.now().toEpochMilli();

        userRequestCounts.compute(userId, (key, value) -> {
            if(value == null || now - value[1] > WINDOW_MS) {
                return new Long[]{1L, now};
            }

            value[0]++;
            return value;
        });

        Long[] state = userRequestCounts.get(userId);
        if(state[0] > MAX_BOOKINGS_PER_DAY) {
            log.warn("Booking rate limit exceeded for user: {}", userId);
            throw new TooManyBookingRequestException(
                    "Too many booking requests. Maximum "
                    + MAX_BOOKINGS_PER_DAY
                    + " bookings allowed per day.");
        }
    }
}

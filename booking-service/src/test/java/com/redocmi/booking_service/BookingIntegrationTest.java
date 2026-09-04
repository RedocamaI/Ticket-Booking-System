package com.redocmi.booking_service;

import com.redocmi.booking_service.client.TrainServiceClient;
import com.redocmi.booking_service.entity.Booking;
import com.redocmi.booking_service.exception.SeatNotAvailableException;
import com.redocmi.booking_service.repository.BookingRepository;
import com.redocmi.booking_service.repository.PaymentRepository;
import com.redocmi.booking_service.service.BookingExpiryScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class BookingIntegrationTest {

    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("booking_test_db")
                    .withUsername("redocmi")
                    .withPassword("password123");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private TrainServiceClient trainServiceClient;

    private final UUID userId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID seatId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void createBooking_shouldSucceed_whenSeatAvailable() throws Exception {
        doNothing().when(trainServiceClient).lockSeat(any());

        mockMvc.perform(post("/api/bookings/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                        {
                            "scheduleId": "%s",
                            "seatId": "%s"
                        }
                        """.formatted(scheduleId, seatId)))
                .andExpect(status().isCreated());

        assertThat(bookingRepository.findAll()).hasSize(1);
        assertThat(bookingRepository.findAll().getFirst().getStatus())
                .isEqualTo(Booking.BookingStatus.PENDING);
    }

    @Test
    void createBooking_shouldFail_whenSeatNotAvailable() throws Exception{
        doThrow(new SeatNotAvailableException(("Seat not available")))
                .when(trainServiceClient).lockSeat(any());

        mockMvc.perform(post("/api/bookings/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                        {
                            "scheduleId": "%s",
                            "seatId": "%s"
                        }
                        """.formatted(scheduleId, seatId)))
                .andExpect(status().isLocked());

        assertThat(bookingRepository.findAll()).isEmpty();
        assertThatThrownBy(() -> trainServiceClient.lockSeat(seatId))
                .isInstanceOf(SeatNotAvailableException.class);
    }

    @Test
    void concurrentBooking_shouldAllowOnlyOne_whenTwoThreadsBookSameSeat() throws Exception {
        doNothing()
                .doThrow(new SeatNotAvailableException("Seat not available"))
                .when(trainServiceClient).lockSeat(any());
        int threadCount = 2;
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try(ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for(int i=0;i<threadCount;i++) {
                executor.submit(() -> {
                    try {
                        latch.await();
                        MvcResult result = mockMvc.perform(post("/api/bookings/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("X-User-Id", userId.toString())
                                        .content("""
                                    {
                                        "scheduleId": "%s",
                                        "seatId": "%s"
                                    }
                                    """.formatted(scheduleId, seatId)))
                                .andReturn();

                        int status = result.getResponse().getStatus();
                        if(status == 201)   successCount.incrementAndGet();
                        else failCount.incrementAndGet();
                    } catch (Exception exception) {
                        failCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            latch.countDown();
            doneLatch.await();
        }

//        Exactly one success and one failure
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    @Test
    void expiryScheduler_shouldCancelExpiredBookings() {
        doNothing().when(trainServiceClient).lockSeat(any());
        doNothing().when(trainServiceClient).releaseSeat(any());
        Booking expiredBooking = Booking.builder()
                .userId(userId)
                .scheduleId(scheduleId)
                .seatId(seatId)
                .status(Booking.BookingStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();
        bookingRepository.save(expiredBooking);

        BookingExpiryScheduler scheduler = new BookingExpiryScheduler(
                bookingRepository, trainServiceClient);
        scheduler.expireStaleBookings();

        Booking updated = bookingRepository.findById(expiredBooking.getId())
                .orElseThrow();
        assertThat(updated.getStatus())
                .isEqualTo(Booking.BookingStatus.CANCELLED);
    }
}

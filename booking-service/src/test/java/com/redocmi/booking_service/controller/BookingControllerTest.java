package com.redocmi.booking_service.controller;

import com.redocmi.booking_service.dto.request.CreateBookingRequest;
import com.redocmi.booking_service.dto.response.BookingResponse;
import com.redocmi.booking_service.exception.SeatNotAvailableException;
import com.redocmi.booking_service.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    private final UUID userId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID seatId = UUID.randomUUID();

    @Test
    void createBooking_shouldSucceed_whenValidRequest() throws Exception {
        BookingResponse mockResponse = BookingResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .scheduleId(scheduleId)
                .seatId(seatId)
                .status("PENDING")
                .bookedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        when(bookingService.createBooking(any(CreateBookingRequest.class), any(UUID.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/bookings/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                        {
                            "scheduleId": "%s",
                            "seatId": "%s"
                        }
                        """.formatted(scheduleId, seatId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createBooking_shouldFail_whenSeatNotAvailable() throws Exception {
        when(bookingService.createBooking(any(CreateBookingRequest.class), any(UUID.class)))
                .thenThrow(new SeatNotAvailableException("Seat is not available"));

        mockMvc.perform(post("/api/bookings/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                        {
                            "scheduleId": "%s",
                            "seatId": "%s"
                        }
                        """.formatted(scheduleId, seatId)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createBooking_shouldFail_whenMissingUserId() throws Exception {
//        requires further checking, it shouldn't return internalServerError
//        it should return BadRequest.
        mockMvc.perform(post("/api/bookings/")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "scheduleId": "%s",
                            "seatId": "%s"
                        }
                        """.formatted(scheduleId, seatId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBookings_shouldReturnList() throws Exception {
        List<BookingResponse> mockResponse = List.of(
                BookingResponse.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .scheduleId(scheduleId)
                        .seatId(seatId)
                        .status("CONFIRMED")
                        .bookedAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusMinutes(10))
                        .build()
        );

        when(bookingService.getBookingsByUser(any(UUID.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/bookings/")
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void createBooking_shouldFail_whenMissingFields() throws Exception {
        mockMvc.perform(post("/api/bookings/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                        {
                            "scheduleId": "%s"
                        }
                        """.formatted(scheduleId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

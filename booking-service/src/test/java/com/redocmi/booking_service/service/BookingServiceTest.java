package com.redocmi.booking_service.service;

import com.redocmi.booking_service.client.TrainServiceClient;
import com.redocmi.booking_service.dto.request.CreateBookingRequest;
import com.redocmi.booking_service.dto.response.BookingResponse;
import com.redocmi.booking_service.entity.Booking;
import com.redocmi.booking_service.exception.BookingExpiredException;
import com.redocmi.booking_service.exception.BookingNotConfirmedException;
import com.redocmi.booking_service.exception.SeatNotAvailableException;
import com.redocmi.booking_service.exception.UnauthorizedException;
import com.redocmi.booking_service.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TrainServiceClient trainServiceClient;

    @InjectMocks
    private BookingService bookingService;

    private final UUID userId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID seatId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @Test
    void createBooking_shouldSucceed_whenSeatAvailable() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setScheduleId(scheduleId);
        request.setSeatId(seatId);

        Booking savedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .seatId(seatId)
                .scheduleId(scheduleId)
                .status(Booking.BookingStatus.PENDING)
                .bookedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        doNothing().when(trainServiceClient).lockSeat(seatId);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request, userId);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getSeatId()).isEqualTo(seatId);
        verify(trainServiceClient).lockSeat(seatId);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_shouldThrow_whenSeatNotAvailable() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setScheduleId(scheduleId);
        request.setSeatId(seatId);

        doThrow(new SeatNotAvailableException("Seat is not available"))
                .when(trainServiceClient).lockSeat(seatId);

        assertThatThrownBy(() -> bookingService.createBooking(request, userId))
                .isInstanceOf(SeatNotAvailableException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getBookingsByUser_shouldReturnList() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .seatId(seatId)
                .scheduleId(scheduleId)
                .status(Booking.BookingStatus.CONFIRMED)
                .bookedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        when(bookingRepository.findByUserId(userId)).thenReturn(List.of(booking));

        List<BookingResponse> mockResponse = bookingService.getBookingsByUser(userId);

        assertThat(mockResponse).hasSize(1);
        assertThat(mockResponse.getFirst().getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void getBookingById_shouldThrow_whenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .userId(userId)
                .seatId(seatId)
                .scheduleId(scheduleId)
                .status(Booking.BookingStatus.CONFIRMED)
                .bookedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.getBookingById(bookingId, otherUserId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void cancelBooking_shouldThrow_whenNotConfirmed() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .userId(userId)
                .seatId(seatId)
                .scheduleId(scheduleId)
                .status(Booking.BookingStatus.PENDING)
                .bookedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, userId))
                .isInstanceOf(BookingNotConfirmedException.class);
    }

    @Test
    void processPayment_shouldThrow_whenBookingExpired() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .userId(userId)
                .seatId(seatId)
                .scheduleId(scheduleId)
                .status(Booking.BookingStatus.PENDING)
                .bookedAt(LocalDateTime.now().minusMinutes(15))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.processPayment(bookingId, userId))
                .isInstanceOf(BookingExpiredException.class);
    }
}

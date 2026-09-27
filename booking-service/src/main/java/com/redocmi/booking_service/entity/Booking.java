package com.redocmi.booking_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "seat_class", nullable = false)
    private String seatClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "booked_at", nullable = false, updatable = false)
    private LocalDateTime bookedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ElementCollection
    @CollectionTable(
            name = "booking_seats",
            joinColumns = @JoinColumn(name = "booking_id")
    )
    @Column(name = "seat_id")
    private List<UUID> seatIds = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        bookedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED
    }
}

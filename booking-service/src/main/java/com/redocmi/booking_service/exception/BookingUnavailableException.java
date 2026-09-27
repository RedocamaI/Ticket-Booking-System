package com.redocmi.booking_service.exception;

public class BookingUnavailableException extends RuntimeException{
    public BookingUnavailableException(String message) {
        super(message);
    }
}

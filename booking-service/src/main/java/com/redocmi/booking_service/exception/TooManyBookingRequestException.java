package com.redocmi.booking_service.exception;

public class TooManyBookingRequestException extends RuntimeException{
    public TooManyBookingRequestException(String message) {
        super(message);
    }
}

package com.redocmi.booking_service.exception;

public class BookingNotPendingException extends RuntimeException{
    public BookingNotPendingException(String message) {
        super(message);
    }
}

package com.redocmi.booking_service.exception;

public class BookingNotConfirmedException extends RuntimeException{
    public BookingNotConfirmedException(String message) {
        super(message);
    }
}

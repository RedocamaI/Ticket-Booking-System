package com.redocmi.booking_service.exception;

public class BookingExpiredException extends RuntimeException{
    public BookingExpiredException(String message){
        super(message);
    }
}

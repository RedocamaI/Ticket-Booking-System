package com.redocmi.train_service.exception;

public class DuplicateTrainException extends RuntimeException{
    public DuplicateTrainException(String message) {
        super(message);
    }
}

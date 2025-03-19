package com.example.userservice.exception;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final String details;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = ErrorCode.APPLICATION_ERROR;
        this.details = message;
    }

    public AppException(String message, HttpStatus status, ErrorCode errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = message;
    }

    public AppException(String message, HttpStatus status, ErrorCode errorCode, String details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }
}
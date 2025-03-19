package com.example.userservice.exception;

/**
 * Standard error codes for API responses
 */
public enum ErrorCode {
    // Authentication errors
    AUTHENTICATION_ERROR,
    UNAUTHORIZED_ERROR,
    FORBIDDEN_ERROR,

    // Validation errors
    VALIDATION_ERROR,
    INVALID_REQUEST,

    // Resource errors
    NOT_FOUND_ERROR,
    RESOURCE_ALREADY_EXISTS,
    RESOURCE_CONFLICT,

    // System errors
    INTERNAL_SERVER_ERROR,
    SERVICE_UNAVAILABLE,

    // Business logic errors
    BUSINESS_ERROR,
    APPLICATION_ERROR
}
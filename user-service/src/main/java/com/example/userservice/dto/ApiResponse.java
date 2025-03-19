package com.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private ErrorDetails error;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorDetails {
        private String code;
        private String details;
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .message("Request processed successfully")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status("success")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String details) {
        ErrorDetails errorDetails = ErrorDetails.builder()
                .code(code)
                .details(details)
                .build();

        return ApiResponse.<T>builder()
                .status("error")
                .message("An error occurred")
                .error(errorDetails)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String code, String details) {
        ErrorDetails errorDetails = ErrorDetails.builder()
                .code(code)
                .details(details)
                .build();

        return ApiResponse.<T>builder()
                .status("error")
                .message(message)
                .error(errorDetails)
                .build();
    }
}
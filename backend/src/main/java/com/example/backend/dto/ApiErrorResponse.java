package com.example.backend.dto;

import java.time.LocalDateTime;

// standard error JSON shape returned by GlobalExceptionHandler
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {
}
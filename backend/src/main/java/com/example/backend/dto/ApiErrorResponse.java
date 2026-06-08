package com.example.backend.dto;

import java.time.LocalDateTime;

// error json vue reads - frontend apiError.ts pulls the message field
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,      // 400, 403, 404 etc
        String error,    // short label like "Bad Request"
        String message,  // the actual text we show the user
        String path) {   // which endpoint failed e.g. /auth/login
}
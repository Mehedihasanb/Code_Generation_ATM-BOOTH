package com.example.backend.dtos;

public record TokenResponse(
        String value,
        long expiration,
        String type
) {}

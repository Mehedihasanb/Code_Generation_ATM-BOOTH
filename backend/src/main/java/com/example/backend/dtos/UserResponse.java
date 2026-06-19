package com.example.backend.dtos;

import com.example.backend.entities.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        LocalDateTime createdAt
) {}

package com.example.backend.dtos;

import com.example.backend.entities.enums.CustomerStatus;
import com.example.backend.entities.enums.UserRole;

import java.time.LocalDateTime;

public record CustomerSummaryResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        CustomerStatus status,
        LocalDateTime createdAt
) {}

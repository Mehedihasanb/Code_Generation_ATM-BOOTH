package com.example.backend.dtos;

import com.example.backend.entities.enums.CustomerStatus;
import com.example.backend.entities.enums.UserRole;

public record CurrentUserResponse(
        int id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        CustomerStatus status,
        String bsn,
        String phoneNumber
) {}

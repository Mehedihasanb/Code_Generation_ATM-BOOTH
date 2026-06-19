package com.example.backend.dtos;

import com.example.backend.entities.enums.CustomerStatus;

public record CustomerProfileResponse(
        int id,
        int userId,
        String bsn,
        String phoneNumber,
        CustomerStatus status
) {}

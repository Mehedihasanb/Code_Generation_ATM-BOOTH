package com.example.backend.dtos;

import com.example.backend.entities.enums.AccountStatus;
import com.example.backend.entities.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmployeeAccountResponse(
        int userId,
        String iban,
        AccountType type,
        BigDecimal balance,
        BigDecimal absoluteTransferLimit,
        BigDecimal dailyTransferLimit,
        AccountStatus status,
        LocalDateTime createdAt
) {}

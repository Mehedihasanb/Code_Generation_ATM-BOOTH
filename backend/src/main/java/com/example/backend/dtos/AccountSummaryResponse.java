package com.example.backend.dtos;

import com.example.backend.entities.enums.AccountStatus;
import com.example.backend.entities.enums.AccountType;

import java.math.BigDecimal;

public record AccountSummaryResponse(
        String iban,
        AccountType type,
        BigDecimal balance,
        AccountStatus status
) {}

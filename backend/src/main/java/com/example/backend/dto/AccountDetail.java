package com.example.backend.dto;

import java.math.BigDecimal;

public record AccountDetail(
        String iban,
        String accountType,
        BigDecimal balance,
        BigDecimal absoluteLimit) {
}
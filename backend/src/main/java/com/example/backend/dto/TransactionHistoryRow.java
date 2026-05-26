package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryRow(
        Long transactionId,
        LocalDateTime timestamp,
        BigDecimal amount,
        String counterpartIban,
        String type,
        String description) {
}
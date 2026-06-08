package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// one row in employee system-wide transaction log (includes who initiated it)
public record SystemTransactionRow(
        Long transactionId,
        LocalDateTime timestamp,
        String fromIban,
        String toIban,
        BigDecimal amount,
        String initiatingUser,
        String type) {
}
package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SystemTransactionRow(
        Long transactionId,
        LocalDateTime timestamp,
        String fromIban,
        String toIban,
        BigDecimal amount,
        String initiatingUser,
        String type) {
}
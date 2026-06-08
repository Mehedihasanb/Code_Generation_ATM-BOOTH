package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// one row in customer transaction history (incoming or outgoing from their perspective)
public record TransactionHistoryRow(
        Long transactionId,
        LocalDateTime timestamp,
        BigDecimal amount,
        String counterpartIban,
        String type,
        String description) {
}
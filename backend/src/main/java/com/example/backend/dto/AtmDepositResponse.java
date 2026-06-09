package com.example.backend.dto;

import java.math.BigDecimal;

/** Response after a successful ATM deposit. */
public record AtmDepositResponse(
		Long transactionId,
		String toIban,
		BigDecimal amount,
		BigDecimal newBalance) {
}

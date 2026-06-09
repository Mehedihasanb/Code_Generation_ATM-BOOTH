package com.example.backend.dto;

import java.math.BigDecimal;

/** Response after a successful ATM withdrawal. */
public record AtmWithdrawResponse(
		Long transactionId,
		String fromIban,
		BigDecimal amount,
		BigDecimal newBalance) {
}

package com.example.backend.dto;

import java.math.BigDecimal;

// ATM deposit success: transaction id and updated checking balance
public record AtmDepositResponse(
		Long transactionId,
		String toIban,
		BigDecimal amount,
		BigDecimal newBalance) {
}

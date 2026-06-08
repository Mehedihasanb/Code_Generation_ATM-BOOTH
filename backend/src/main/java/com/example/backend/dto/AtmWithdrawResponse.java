package com.example.backend.dto;

import java.math.BigDecimal;

// ATM withdraw success: transaction id and updated checking balance
public record AtmWithdrawResponse(
		Long transactionId,
		String fromIban,
		BigDecimal amount,
		BigDecimal newBalance) {
}

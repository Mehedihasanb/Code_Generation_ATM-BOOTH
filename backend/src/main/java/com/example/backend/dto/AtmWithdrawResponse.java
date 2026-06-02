package com.example.backend.dto;

import java.math.BigDecimal;

public record AtmWithdrawResponse(
		Long transactionId,
		String fromIban,
		BigDecimal amount,
		BigDecimal newBalance) {
}

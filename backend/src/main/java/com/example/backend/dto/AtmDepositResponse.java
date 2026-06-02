package com.example.backend.dto;

import java.math.BigDecimal;

public record AtmDepositResponse(
		Long transactionId,
		String toIban,
		BigDecimal amount,
		BigDecimal newBalance) {
}

package com.example.backend.web.dto;

import java.math.BigDecimal;

public record CustomerAccountRow(
	String iban,
	String accountType,
	boolean active,
	BigDecimal balance,
	BigDecimal minimumAllowedBalance,
	BigDecimal dailyOutgoingTransferLimit
) {
}

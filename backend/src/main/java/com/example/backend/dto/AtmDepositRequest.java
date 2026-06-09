package com.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Request body for POST /atm/deposit. */
public record AtmDepositRequest(
		@NotNull(message = "amount is required")
		@DecimalMin(value = "0.01", message = "amount must be greater than zero")
		@Digits(integer = 12, fraction = 2, message = "amount has too many digits")
		BigDecimal amount,

		// Optional: required only when customer has more than one CHECKING account
		String toIban) {
}

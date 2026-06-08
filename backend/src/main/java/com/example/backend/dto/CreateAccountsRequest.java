package com.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// US-10: employee opens checking + savings when approving a pending customer
public record CreateAccountsRequest(
		@NotNull(message = "customerRegistrationId is required") Long customerRegistrationId,

		@NotNull(message = "dailyOutgoingTransferLimit is required") @DecimalMin(value = "0.01", message = "dailyOutgoingTransferLimit must be greater than zero") @Digits(integer = 12, fraction = 2, message = "dailyOutgoingTransferLimit has too many digits") BigDecimal dailyOutgoingTransferLimit,

		@NotNull(message = "minimumAllowedBalance is required") @DecimalMin(value = "0", inclusive = true, message = "minimumAllowedBalance must be zero or negative (overdraft floor)") @Digits(integer = 12, fraction = 2, message = "minimumAllowedBalance has too many digits") BigDecimal minimumAllowedBalance) {
}

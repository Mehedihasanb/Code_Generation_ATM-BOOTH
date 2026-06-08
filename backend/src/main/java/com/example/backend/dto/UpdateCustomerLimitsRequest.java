package com.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

// employee updates daily transfer limit and max balance cap on customer accounts
public record UpdateCustomerLimitsRequest(
		@DecimalMin(value = "0", inclusive = true, message = "absoluteLimit must be zero or greater")
		@Digits(integer = 12, fraction = 2, message = "absoluteLimit has too many digits")
		BigDecimal absoluteLimit,

		@DecimalMin(value = "0.01", message = "dailyOutgoingTransferLimit must be greater than zero")
		@Digits(integer = 12, fraction = 2, message = "dailyOutgoingTransferLimit has too many digits")
		BigDecimal dailyOutgoingTransferLimit) {
}

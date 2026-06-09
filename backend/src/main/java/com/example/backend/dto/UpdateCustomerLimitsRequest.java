package com.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

/**
 * Request body for PUT /users/{id}/limits.
 * Both fields are optional, but at least one must be sent (checked in the service).
 */
public record UpdateCustomerLimitsRequest(
		// Max balance cap stored as minimumAllowedBalance on each account
		@DecimalMin(value = "0", inclusive = true, message = "absoluteLimit must be zero or greater")
		@Digits(integer = 12, fraction = 2, message = "absoluteLimit has too many digits")
		BigDecimal absoluteLimit,

		// Max amount that can move in/out per day on each account
		@DecimalMin(value = "0.01", message = "dailyOutgoingTransferLimit must be greater than zero")
		@Digits(integer = 12, fraction = 2, message = "dailyOutgoingTransferLimit has too many digits")
		BigDecimal dailyOutgoingTransferLimit) {
}

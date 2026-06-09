package com.example.backend.dto;

import java.math.BigDecimal;

/** Response after PUT /users/{id}/limits — confirms what was saved. */
public record UpdateCustomerLimitsResponse(
		Long customerId,
		BigDecimal absoluteLimit,
		BigDecimal dailyOutgoingTransferLimit,
		int accountsUpdated) {
}

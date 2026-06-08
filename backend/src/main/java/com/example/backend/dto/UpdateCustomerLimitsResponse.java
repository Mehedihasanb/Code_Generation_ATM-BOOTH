package com.example.backend.dto;

import java.math.BigDecimal;

// confirmation after limits update (how many accounts were touched)
public record UpdateCustomerLimitsResponse(
		Long customerId,
		BigDecimal absoluteLimit,
		BigDecimal dailyOutgoingTransferLimit,
		int accountsUpdated) {
}

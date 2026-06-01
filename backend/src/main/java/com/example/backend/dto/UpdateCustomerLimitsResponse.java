package com.example.backend.dto;

import java.math.BigDecimal;

public record UpdateCustomerLimitsResponse(
		Long customerId,
		BigDecimal absoluteLimit,
		BigDecimal dailyOutgoingTransferLimit,
		int accountsUpdated) {
}

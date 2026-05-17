package com.example.backend.dto;

import java.util.List;

public record CreatedAccountsResponse(
		long customerRegistrationId,
		String customerApprovalStatus,
		List<CreatedAccountLine> createdAccounts) {
}

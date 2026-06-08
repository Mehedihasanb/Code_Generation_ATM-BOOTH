package com.example.backend.dto;

import java.util.List;

// US-10 response: customer now APPROVED plus the new IBANs that were created
public record CreatedAccountsResponse(
		long customerRegistrationId,
		String customerApprovalStatus,
		List<CreatedAccountLine> createdAccounts) {
}

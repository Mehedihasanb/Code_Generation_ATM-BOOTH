package com.example.backend.dto;

import java.util.List;

// one customer row for the employee directory. customerApprovalStatus is PENDING, APPROVED or DENIED
public record CustomerDirectoryRow(
		long id,
		String firstName,
		String lastName,
		String email,
		String customerApprovalStatus,
		List<CustomerAccountRow> accounts) {
}

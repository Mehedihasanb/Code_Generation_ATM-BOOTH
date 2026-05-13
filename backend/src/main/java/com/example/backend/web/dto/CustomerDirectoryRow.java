package com.example.backend.web.dto;

import java.util.List;

/** Customer row for employee directory; {@code customerApprovalStatus} is PENDING, APPROVED, or DENIED. */
public record CustomerDirectoryRow(
	long id,
	String firstName,
	String lastName,
	String email,
	String customerApprovalStatus,
	List<CustomerAccountRow> accounts
) {
}

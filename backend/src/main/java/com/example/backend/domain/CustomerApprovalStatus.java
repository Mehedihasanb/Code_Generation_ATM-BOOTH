package com.example.backend.domain;

/**
 * Customer lifecycle after self-registration. Employees approve (open accounts) or deny.
 * Not used for {@code EMPLOYEE} rows (column is null in the database).
 */
public enum CustomerApprovalStatus {
	PENDING,
	APPROVED,
	DENIED
}

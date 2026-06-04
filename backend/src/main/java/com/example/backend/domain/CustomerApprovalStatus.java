package com.example.backend.domain;

// status of a customer after they register. employees approve or deny. null for employees
public enum CustomerApprovalStatus {
	PENDING,
	APPROVED,
	DENIED
}

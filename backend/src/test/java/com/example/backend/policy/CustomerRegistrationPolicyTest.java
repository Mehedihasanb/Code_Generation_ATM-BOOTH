package com.example.backend.policy;

import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.RegistrationDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// unit tests for the rules in CustomerRegistrationPolicy.
// no mocks needed, the policy only looks at a UserRegistration object
class CustomerRegistrationPolicyTest {

	private final CustomerRegistrationPolicy policy = new CustomerRegistrationPolicy();

	private UserRegistration user(String role, CustomerApprovalStatus status) {
		return new UserRegistration("First", "Last", "user@example.com", "hashed", role, status, "123456789", "+31 6 00000000");
	}

	@Test
	void requireCustomer_throwsForNonCustomer() {
		assertThrows(BadRequestException.class, () -> policy.requireCustomer(user("EMPLOYEE", null)));
	}

	@Test
	void requireCustomer_passesForCustomer() {
		assertDoesNotThrow(() -> policy.requireCustomer(user("CUSTOMER", CustomerApprovalStatus.PENDING)));
	}

	@Test
	void requirePendingApproval_throwsWhenNotPending() {
		assertThrows(BadRequestException.class,
			() -> policy.requirePendingApproval(user("CUSTOMER", CustomerApprovalStatus.APPROVED)));
	}

	@Test
	void requirePendingApproval_passesWhenPending() {
		assertDoesNotThrow(() -> policy.requirePendingApproval(user("CUSTOMER", CustomerApprovalStatus.PENDING)));
	}

	@Test
	void requirePendingForDeny_throwsWhenNotPending() {
		assertThrows(BadRequestException.class,
			() -> policy.requirePendingForDeny(user("CUSTOMER", CustomerApprovalStatus.DENIED)));
	}

	@Test
	void requireNotDeniedForLogin_throwsForDeniedCustomer() {
		assertThrows(RegistrationDeniedException.class,
			() -> policy.requireNotDeniedForLogin(user("CUSTOMER", CustomerApprovalStatus.DENIED)));
	}

	@Test
	void requireNotDeniedForLogin_passesForApprovedCustomer() {
		assertDoesNotThrow(() -> policy.requireNotDeniedForLogin(user("CUSTOMER", CustomerApprovalStatus.APPROVED)));
	}
}

package com.example.backend.policy;

import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.exception.BadRequestException;
import com.example.backend.repository.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// unit tests for AccountOpeningPolicy. the repository is mocked so we test the rules without a database
@ExtendWith(MockitoExtension.class)
class AccountOpeningPolicyTest {

	@Mock
	private BankAccountRepository bankAccountRepository;

	private AccountOpeningPolicy accountOpeningPolicy;

	@BeforeEach
	void setUp() {
		accountOpeningPolicy = new AccountOpeningPolicy(new CustomerRegistrationPolicy(), bankAccountRepository);
	}

	private UserRegistration pendingCustomer() {
		return new UserRegistration("Pat", "Pending", "pat@example.com", "hashed", "CUSTOMER",
			CustomerApprovalStatus.PENDING, "123456789", "+31 6 00000000");
	}

	@Test
	void requireEligible_passesForPendingCustomerWithoutAccounts() {
		when(bankAccountRepository.existsByOwner_Id(any())).thenReturn(false);
		assertDoesNotThrow(() -> accountOpeningPolicy.requireEligibleForNewAccounts(pendingCustomer()));
	}

	@Test
	void requireEligible_throwsWhenCustomerAlreadyHasAccounts() {
		when(bankAccountRepository.existsByOwner_Id(any())).thenReturn(true);
		assertThrows(BadRequestException.class,
			() -> accountOpeningPolicy.requireEligibleForNewAccounts(pendingCustomer()));
	}

	@Test
	void requireEligible_throwsWhenCustomerNotPending() {
		UserRegistration approved = new UserRegistration("Al", "Ready", "al@example.com", "hashed", "CUSTOMER",
			CustomerApprovalStatus.APPROVED, "123456789", "+31 6 00000000");
		assertThrows(BadRequestException.class,
			() -> accountOpeningPolicy.requireEligibleForNewAccounts(approved));
	}
}

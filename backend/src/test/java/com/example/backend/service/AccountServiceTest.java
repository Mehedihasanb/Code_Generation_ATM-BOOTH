package com.example.backend.service;

import com.example.backend.domain.AccountType;
import com.example.backend.domain.BankAccount;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.dto.CreateAccountsRequest;
import com.example.backend.dto.CreatedAccountsResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.policy.AccountOpeningPolicy;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.UserRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// unit tests for AccountService. all dependencies (repositories, policy, IBAN service)
// are mocked so we only test the service's own logic
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	private BankAccountRepository bankAccountRepository;
	@Mock
	private UserRegistrationRepository userRegistrationRepository;
	@Mock
	private AccountOpeningPolicy accountOpeningPolicy;
	@Mock
	private IbanAllocationService ibanAllocationService;

	@InjectMocks
	private AccountService accountService;

	@Test
	void createAccounts_opensCheckingAndSavingsAndApprovesCustomer() {
		UserRegistration customer = org.mockito.Mockito.mock(UserRegistration.class);
		when(customer.getId()).thenReturn(1L);
		when(userRegistrationRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(ibanAllocationService.allocateUniqueDutchIban())
			.thenReturn("NL01INHO0000000001", "NL01INHO0000000002");

		CreateAccountsRequest request =
			new CreateAccountsRequest(1L, new BigDecimal("2500.00"), new BigDecimal("0.00"));

		CreatedAccountsResponse response = accountService.createCheckingAndSavingsAccounts(request);

		assertEquals(2, response.createdAccounts().size());
		assertEquals("APPROVED", response.customerApprovalStatus());
		verify(customer).setCustomerApprovalStatus(CustomerApprovalStatus.APPROVED);
		verify(bankAccountRepository, times(2)).save(any(BankAccount.class));
	}

	@Test
	void createAccounts_throwsWhenCustomerNotFound() {
		when(userRegistrationRepository.findById(99L)).thenReturn(Optional.empty());
		CreateAccountsRequest request =
			new CreateAccountsRequest(99L, new BigDecimal("2500.00"), new BigDecimal("0.00"));

		assertThrows(ResourceNotFoundException.class,
			() -> accountService.createCheckingAndSavingsAccounts(request));
	}

	@Test
	void closeAccount_throwsWhenIbanUnknown() {
		when(bankAccountRepository.findByIban("NL99INHO0000000000")).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class,
			() -> accountService.closeAccountByIban("NL99INHO0000000000"));
	}

	@Test
	void closeAccount_deactivatesActiveAccount() {
		BankAccount account = new BankAccount(null, "NL01INHO0000000001", AccountType.CHECKING, true,
			BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN);
		when(bankAccountRepository.findByIban("NL01INHO0000000001")).thenReturn(Optional.of(account));

		accountService.closeAccountByIban("NL01INHO0000000001");

		assertFalse(account.isActive());
		verify(bankAccountRepository).save(account);
	}

	@Test
	void closeAccount_alreadyClosedDoesNothing() {
		BankAccount account = new BankAccount(null, "NL01INHO0000000001", AccountType.CHECKING, false,
			BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN);
		when(bankAccountRepository.findByIban("NL01INHO0000000001")).thenReturn(Optional.of(account));

		accountService.closeAccountByIban("NL01INHO0000000001");

		verify(bankAccountRepository, never()).save(any(BankAccount.class));
	}
}

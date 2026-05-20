package com.example.backend.service;

import com.example.backend.domain.AccountType;
import com.example.backend.domain.BankAccount;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.policy.AccountOpeningPolicy;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.dto.AccountSummaryResponse;
import com.example.backend.dto.CreateAccountsRequest;
import com.example.backend.dto.CreatedAccountLine;
import com.example.backend.dto.CreatedAccountsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountService {

	private final BankAccountRepository bankAccountRepository;
	private final UserRegistrationRepository userRegistrationRepository;
	private final AccountOpeningPolicy accountOpeningPolicy;
	private final IbanAllocationService ibanAllocationService;

	public AccountService(
		BankAccountRepository bankAccountRepository,
		UserRegistrationRepository userRegistrationRepository,
		AccountOpeningPolicy accountOpeningPolicy,
		IbanAllocationService ibanAllocationService
	) {
		this.bankAccountRepository = bankAccountRepository;
		this.userRegistrationRepository = userRegistrationRepository;
		this.accountOpeningPolicy = accountOpeningPolicy;
		this.ibanAllocationService = ibanAllocationService;
	}

	@Transactional
	public CreatedAccountsResponse createCheckingAndSavingsAccounts(CreateAccountsRequest createAccountsRequest) {
		UserRegistration customer = userRegistrationRepository.findById(createAccountsRequest.customerRegistrationId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

		accountOpeningPolicy.requireEligibleForNewAccounts(customer);

		BigDecimal zeroBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		BigDecimal minimumAllowedBalance = createAccountsRequest.minimumAllowedBalance()
			.setScale(2, RoundingMode.HALF_UP);
		BigDecimal dailyOutgoingTransferLimit = createAccountsRequest.dailyOutgoingTransferLimit()
			.setScale(2, RoundingMode.HALF_UP);

		String checkingIban = ibanAllocationService.allocateUniqueDutchIban();
		String savingsIban = ibanAllocationService.allocateUniqueDutchIban();

		BankAccount checkingAccount = new BankAccount(
			customer,
			checkingIban,
			AccountType.CHECKING,
			true,
			zeroBalance,
			minimumAllowedBalance,
			dailyOutgoingTransferLimit
		);
		BankAccount savingsAccount = new BankAccount(
			customer,
			savingsIban,
			AccountType.SAVINGS,
			true,
			zeroBalance,
			minimumAllowedBalance,
			dailyOutgoingTransferLimit
		);

		bankAccountRepository.save(checkingAccount);
		bankAccountRepository.save(savingsAccount);

		customer.setCustomerApprovalStatus(CustomerApprovalStatus.APPROVED);
		userRegistrationRepository.save(customer);

		List<CreatedAccountLine> createdAccounts = new ArrayList<>();
		createdAccounts.add(new CreatedAccountLine(checkingIban, AccountType.CHECKING.name()));
		createdAccounts.add(new CreatedAccountLine(savingsIban, AccountType.SAVINGS.name()));

		return new CreatedAccountsResponse(
			customer.getId(),
			CustomerApprovalStatus.APPROVED.name(),
			createdAccounts
		);
	}

	@Transactional
	public void closeAccountByIban(String ibanFromPath) {
		String normalizedIban = ibanFromPath.trim().toUpperCase();

		BankAccount bankAccount = bankAccountRepository.findByIban(normalizedIban)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown IBAN"));

		if (!bankAccount.isActive()) {
			return;
		}

		bankAccount.setActive(false);
		bankAccountRepository.save(bankAccount);
	}

	@Transactional(readOnly = true)
	public AccountSummaryResponse getMyAccounts(String email) {
		UserRegistration customer = userRegistrationRepository.findByEmail(email)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

		List<BankAccount> accounts = bankAccountRepository.findAllByOwner_Id(customer.getId());
		return AccountSummaryResponse.fromCustomerAndAccounts(customer, accounts);
	}
}

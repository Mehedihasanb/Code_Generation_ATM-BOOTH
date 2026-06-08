package com.example.backend.service;

import com.example.backend.domain.AccountType;
import com.example.backend.domain.BankAccount;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.policy.AccountOpeningPolicy;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.dto.AccountSummaryResponse;
import com.example.backend.dto.CheckingAccountOptionRow;
import com.example.backend.dto.CreateAccountsRequest;
import com.example.backend.dto.CreatedAccountLine;
import com.example.backend.dto.CreatedAccountsResponse;
import com.example.backend.dto.CustomerAccountRow;
import com.example.backend.dto.CustomerDirectoryRow;
import com.example.backend.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// my account service - approve (US-10), close (US-11), my accounts, directory search
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
			IbanAllocationService ibanAllocationService) {
		this.bankAccountRepository = bankAccountRepository;
		this.userRegistrationRepository = userRegistrationRepository;
		this.accountOpeningPolicy = accountOpeningPolicy;
		this.ibanAllocationService = ibanAllocationService;
	}

	// US-10 employee approve - POST /accounts, opens checking + savings
	@Transactional
	public CreatedAccountsResponse createCheckingAndSavingsAccounts(CreateAccountsRequest createAccountsRequest) {
		// load the pending customer id that came from the approve form
		UserRegistration customer = userRegistrationRepository.findById(createAccountsRequest.customerRegistrationId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

		// throws if not PENDING or if they already got accounts somehow
		accountOpeningPolicy.requireEligibleForNewAccounts(customer);

		// started at 0.00 originally - changed to 1000 so we can demo transfers without funding first
		BigDecimal startingBalance = new BigDecimal("1000.00").setScale(2, RoundingMode.HALF_UP);
		// limits from employee form on service desk, rounded to cents
		BigDecimal minimumAllowedBalance = createAccountsRequest.minimumAllowedBalance()
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal dailyOutgoingTransferLimit = createAccountsRequest.dailyOutgoingTransferLimit()
				.setScale(2, RoundingMode.HALF_UP);

		String checkingIban = ibanAllocationService.allocateUniqueDutchIban();
		String savingsIban = ibanAllocationService.allocateUniqueDutchIban();

		// BankAccount constructor order: owner, iban, type, active, balance, min balance, daily limit
		BankAccount checkingAccount = new BankAccount(
				customer,
				checkingIban,
				AccountType.CHECKING,
				true,
				startingBalance,
				minimumAllowedBalance,
				dailyOutgoingTransferLimit);
		BankAccount savingsAccount = new BankAccount(
				customer,
				savingsIban,
				AccountType.SAVINGS,
				true,
				startingBalance,
				minimumAllowedBalance,
				dailyOutgoingTransferLimit);

		bankAccountRepository.save(checkingAccount);
		bankAccountRepository.save(savingsAccount);

		// this is the real approve register only made them PENDING
		customer.setCustomerApprovalStatus(CustomerApprovalStatus.APPROVED);
		userRegistrationRepository.save(customer);

		// response dto for service desk not entities, just iban + type per account
		List<CreatedAccountLine> createdAccounts = new ArrayList<>();
		createdAccounts.add(new CreatedAccountLine(checkingIban, AccountType.CHECKING.name()));
		createdAccounts.add(new CreatedAccountLine(savingsIban, AccountType.SAVINGS.name()));

		return new CreatedAccountsResponse(
				customer.getId(),
				CustomerApprovalStatus.APPROVED.name(),
				createdAccounts);
	}

	// US-11 employee close account - PUT /accounts/{iban}/close
	@Transactional
	public void closeAccountByIban(String ibanFromPath) {
		String normalizedIban = ibanFromPath.trim().toUpperCase();

		BankAccount bankAccount = bankAccountRepository.findByIban(normalizedIban)
				.orElseThrow(() -> new ResourceNotFoundException("Unknown IBAN"));

		if (!bankAccount.isActive()) {
			return; // already closed, nothing to do
		}

		bankAccount.setActive(false);
		bankAccountRepository.save(bankAccount);
	}

	// logged in customer sees their accounts - GET /accounts/mine
	@Transactional(readOnly = true)
	public AccountSummaryResponse getMyAccounts(String email) {
		UserRegistration customer = userRegistrationRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

		List<BankAccount> accounts = bankAccountRepository.findAllByOwner_Id(customer.getId());
		return AccountSummaryResponse.fromCustomerAndAccounts(customer, accounts);
	}

	// directory name search called from UserController when employee searches first + last name
	@Transactional(readOnly = true)
	public List<CustomerDirectoryRow> searchCustomersByName(String firstName, String lastName) {
		// both names must match, ignoreCase so Dave = dave
		List<UserRegistration> users = userRegistrationRepository
				.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim());

		return users.stream().map(user -> {
			List<BankAccount> userAccounts = bankAccountRepository.findAllByOwner_Id(user.getId());

			// BankAccount entity -> CustomerAccountRow dto, closed accounts filtered out
			List<CustomerAccountRow> activeAccountRows = userAccounts.stream()
					.filter(BankAccount::isActive) // inactive = employee closed it, dont show for transfer
					.map(CustomerAccountRow::fromBankAccount)
					.toList();

			// one customer row for the directory table json
			return new CustomerDirectoryRow(
					user.getId(),
					user.getFirstName(),
					user.getLastName(),
					user.getEmail(),
					user.getCustomerApprovalStatus().name(),
					activeAccountRows);
		}).toList();
	}

	// force transfer dropdown - GET /accounts/checking-options, employee picks from/to account
	public List<CheckingAccountOptionRow> getAllCheckingAccountsForDropdown() {
		return bankAccountRepository.findAll().stream()
				.filter(acc -> "CHECKING".equalsIgnoreCase(acc.getAccountType().name())) // savings not allowed here
				.map(acc -> new CheckingAccountOptionRow(
						acc.getIban(),
						acc.getOwner().getFirstName() + " " + acc.getOwner().getLastName())) // label for dropdown
				.collect(Collectors.toList());
	}
}

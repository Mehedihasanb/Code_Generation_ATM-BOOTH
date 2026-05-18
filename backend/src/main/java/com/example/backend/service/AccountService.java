package com.example.backend.service;

import com.example.backend.domain.AccountType;
import com.example.backend.domain.BankAccount;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.dto.CreateAccountsRequest;
import com.example.backend.dto.CreatedAccountLine;
import com.example.backend.dto.CreatedAccountsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountService {

	private final BankAccountRepository bankAccountRepository;
	private final UserRegistrationRepository userRegistrationRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public AccountService(
		BankAccountRepository bankAccountRepository,
		UserRegistrationRepository userRegistrationRepository
	) {
		this.bankAccountRepository = bankAccountRepository;
		this.userRegistrationRepository = userRegistrationRepository;
	}

	@Transactional
	public CreatedAccountsResponse createCheckingAndSavingsAccounts(CreateAccountsRequest createAccountsRequest) {
		// Load pending customer
		UserRegistration customer = userRegistrationRepository.findById(createAccountsRequest.customerRegistrationId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

		if (!"CUSTOMER".equals(customer.getRole())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only customers can receive bank accounts");
		}

		if (customer.getCustomerApprovalStatus() != CustomerApprovalStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Customer registration must be pending before opening accounts");
		}

		if (bankAccountRepository.existsByOwner_Id(customer.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer already has bank accounts");
		}

		BigDecimal zeroBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		BigDecimal minimumAllowedBalance = createAccountsRequest.minimumAllowedBalance()
			.setScale(2, RoundingMode.HALF_UP);
		BigDecimal dailyOutgoingTransferLimit = createAccountsRequest.dailyOutgoingTransferLimit()
			.setScale(2, RoundingMode.HALF_UP);

		// US-10: create checking + savings with demo IBANs
		String checkingIban = generateUniqueDemoIban();
		String savingsIban = generateUniqueDemoIban();

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

		// US-11: already closed is fine, nothing to do
		if (!bankAccount.isActive()) {
			return;
		}

		bankAccount.setActive(false);
		bankAccountRepository.save(bankAccount);
	}

	private String generateUniqueDemoIban() {
		for (int attempt = 0; attempt < 100; attempt++) {
			int checkDigits = secureRandom.nextInt(100);
			int accountDigits = secureRandom.nextInt(1_000_000_000);
			String candidateIban = "NL" + String.format("%02d", checkDigits) + "INHO0"
				+ String.format("%09d", accountDigits);

			if (bankAccountRepository.findByIban(candidateIban).isEmpty()) {
				return candidateIban;
			}
		}

		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate a unique demo IBAN");
	}
}

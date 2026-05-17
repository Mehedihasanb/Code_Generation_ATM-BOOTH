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
import java.util.List;

@Service
public class AccountService {

	private final BankAccountRepository bankAccountRepository;
	private final UserRegistrationRepository userRegistrationRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public AccountService(
			BankAccountRepository bankAccountRepository,
			UserRegistrationRepository userRegistrationRepository) {
		this.bankAccountRepository = bankAccountRepository;
		this.userRegistrationRepository = userRegistrationRepository;
	}

	@Transactional
	public CreatedAccountsResponse createCheckingAndSavingsAccounts(CreateAccountsRequest createAccountsRequest) {
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
		BigDecimal minimumAllowedBalance = createAccountsRequest.minimumAllowedBalance().setScale(2,
				RoundingMode.HALF_UP);
		BigDecimal dailyOutgoingTransferLimit = createAccountsRequest.dailyOutgoingTransferLimit().setScale(2,
				RoundingMode.HALF_UP);

		String checkingIban = generateUniqueDemoIban();
		String savingsIban = generateUniqueDemoIban();

		BankAccount checkingAccount = new BankAccount(
				customer,
				checkingIban,
				AccountType.CHECKING,
				true,
				zeroBalance,
				minimumAllowedBalance,
				dailyOutgoingTransferLimit);
		BankAccount savingsAccount = new BankAccount(
				customer,
				savingsIban,
				AccountType.SAVINGS,
				true,
				zeroBalance,
				minimumAllowedBalance,
				dailyOutgoingTransferLimit);

		bankAccountRepository.saveAll(List.of(checkingAccount, savingsAccount));

		customer.setCustomerApprovalStatus(CustomerApprovalStatus.APPROVED);
		userRegistrationRepository.save(customer);

		return new CreatedAccountsResponse(
				customer.getId(),
				CustomerApprovalStatus.APPROVED.name(),
				List.of(
						new CreatedAccountLine(checkingIban, AccountType.CHECKING.name()),
						new CreatedAccountLine(savingsIban, AccountType.SAVINGS.name())));
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

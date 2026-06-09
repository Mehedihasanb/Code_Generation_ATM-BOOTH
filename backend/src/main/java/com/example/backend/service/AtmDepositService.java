package com.example.backend.service;

import com.example.backend.domain.AccountType;
import com.example.backend.domain.BankAccount;
import com.example.backend.domain.Transaction;
import com.example.backend.domain.UserRegistration;
import com.example.backend.dto.AtmDepositRequest;
import com.example.backend.dto.AtmDepositResponse;
import com.example.backend.policy.TransferAuthorizationPolicy;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.TransactionRepository;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.support.AtmConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ATM deposit logic.
 * Money moves from system ATM account -> customer CHECKING account.
 * Enforces absolute balance cap and daily incoming limit. Records a transaction.
 */
@Service
public class AtmDepositService {

	private final BankAccountRepository bankAccountRepository;
	private final TransactionRepository transactionRepository;
	private final UserRegistrationRepository userRegistrationRepository;
	private final TransferAuthorizationPolicy transferAuthorizationPolicy;

	public AtmDepositService(
			BankAccountRepository bankAccountRepository,
			TransactionRepository transactionRepository,
			UserRegistrationRepository userRegistrationRepository,
			TransferAuthorizationPolicy transferAuthorizationPolicy) {
		this.bankAccountRepository = bankAccountRepository;
		this.transactionRepository = transactionRepository;
		this.userRegistrationRepository = userRegistrationRepository;
		this.transferAuthorizationPolicy = transferAuthorizationPolicy;
	}

	@Transactional
	public AtmDepositResponse deposit(AtmDepositRequest request, String customerEmail) {
		UserRegistration customer = userRegistrationRepository.findByEmail(customerEmail.trim().toLowerCase())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

		BankAccount toAccount = resolveDepositAccount(customer, request.toIban());
		BankAccount systemAtmAccount = bankAccountRepository.findByIban(AtmConstants.SYSTEM_ATM_IBAN)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.SERVICE_UNAVAILABLE, "ATM is not available right now"));

		transferAuthorizationPolicy.requireActiveAccounts(systemAtmAccount, toAccount);

		if (!AccountType.CHECKING.equals(toAccount.getAccountType())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"ATM deposits are only allowed to CHECKING accounts");
		}

		BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

		// Absolute limit: balance after deposit must not exceed minimumAllowedBalance (max cap)
		BigDecimal projectedBalance = toAccount.getBalance().add(amount);
		if (projectedBalance.compareTo(toAccount.getMinimumAllowedBalance()) > 0) {
			BigDecimal remainingSpace = toAccount.getMinimumAllowedBalance().subtract(toAccount.getBalance());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Deposit exceeds your absolute limit. This account can only accept €"
							+ formatAmount(remainingSpace.max(BigDecimal.ZERO)) + " more.");
		}

		// Daily limit: sum of money received IN today + this deposit must stay within limit
		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		BigDecimal totalIncomingToday = transactionRepository.sumIncomingTransactionsToday(toAccount, startOfDay);
		BigDecimal projectedIncomingTotal = totalIncomingToday.add(amount);
		if (projectedIncomingTotal.compareTo(toAccount.getDailyOutgoingTransferLimit()) > 0) {
			BigDecimal remainingDailyLimit = toAccount.getDailyOutgoingTransferLimit().subtract(totalIncomingToday);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Deposit exceeds the daily limit. Only €"
							+ formatAmount(remainingDailyLimit.max(BigDecimal.ZERO))
							+ " more can be deposited today.");
		}

		// Update balances: ATM pool loses money, customer gains it
		systemAtmAccount.setBalance(systemAtmAccount.getBalance().subtract(amount));
		toAccount.setBalance(projectedBalance);
		bankAccountRepository.save(systemAtmAccount);
		bankAccountRepository.save(toAccount);

		Transaction transaction = new Transaction(
				systemAtmAccount,
				toAccount,
				amount,
				AtmConstants.DEPOSIT_DESCRIPTION,
				customer);
		transactionRepository.save(transaction);

		return new AtmDepositResponse(
				transaction.getId(),
				toAccount.getIban(),
				amount,
				toAccount.getBalance());
	}

	/** Same account resolution as withdraw, but for the destination (toIban) CHECKING account. */
	private BankAccount resolveDepositAccount(UserRegistration customer, String toIban) {
		if (toIban != null && !toIban.isBlank()) {
			String normalizedIban = toIban.trim().toUpperCase();
			BankAccount account = bankAccountRepository.findByIban(normalizedIban)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
			if (!account.getOwner().getId().equals(customer.getId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this account");
			}
			return account;
		}

		List<BankAccount> checkingAccounts = bankAccountRepository.findAllByOwner_Id(customer.getId()).stream()
				.filter(account -> AccountType.CHECKING.equals(account.getAccountType()))
				.filter(BankAccount::isActive)
				.toList();

		if (checkingAccounts.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active checking account found for deposit");
		}
		if (checkingAccounts.size() > 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Multiple checking accounts found; specify toIban");
		}
		return checkingAccounts.getFirst();
	}

	private String formatAmount(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
	}
}

package com.example.backend.service;

import com.example.backend.domain.AccountType;
import com.example.backend.domain.BankAccount;
import com.example.backend.domain.Transaction;
import com.example.backend.domain.UserRegistration;
import com.example.backend.dto.AtmWithdrawRequest;
import com.example.backend.dto.AtmWithdrawResponse;
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

@Service
public class AtmWithdrawService {

	private final BankAccountRepository bankAccountRepository;
	private final TransactionRepository transactionRepository;
	private final UserRegistrationRepository userRegistrationRepository;
	private final TransferAuthorizationPolicy transferAuthorizationPolicy;

	public AtmWithdrawService(
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
	public AtmWithdrawResponse withdraw(AtmWithdrawRequest request, String customerEmail) {
		UserRegistration customer = userRegistrationRepository.findByEmail(customerEmail.trim().toLowerCase())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

		BankAccount fromAccount = resolveWithdrawalAccount(customer, request.fromIban());
		BankAccount systemAtmAccount = bankAccountRepository.findByIban(AtmConstants.SYSTEM_ATM_IBAN)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.SERVICE_UNAVAILABLE, "ATM is not available right now"));

		transferAuthorizationPolicy.requireCanInitiateFromAccount(customer, fromAccount);
		transferAuthorizationPolicy.requireActiveAccounts(fromAccount, systemAtmAccount);

		if (!AccountType.CHECKING.equals(fromAccount.getAccountType())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"ATM withdrawals are only allowed from CHECKING accounts");
		}

		BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

		BigDecimal newBalance = fromAccount.getBalance().subtract(amount);
		if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Insufficient funds. Maximum available to withdraw is €"
							+ formatAmount(fromAccount.getBalance()));
		}

		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		BigDecimal totalOutgoingToday = transactionRepository.sumOutgoingTransactionsToday(fromAccount, startOfDay);
		BigDecimal projectedTotal = totalOutgoingToday.add(amount);
		if (projectedTotal.compareTo(fromAccount.getDailyOutgoingTransferLimit()) > 0) {
			BigDecimal remainingDailyLimit = fromAccount.getDailyOutgoingTransferLimit().subtract(totalOutgoingToday);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Withdrawal exceeds the daily limit. Only €"
							+ formatAmount(remainingDailyLimit.max(BigDecimal.ZERO))
							+ " more can be withdrawn today.");
		}

		fromAccount.setBalance(newBalance);
		systemAtmAccount.setBalance(systemAtmAccount.getBalance().add(amount));
		bankAccountRepository.save(fromAccount);
		bankAccountRepository.save(systemAtmAccount);

		Transaction transaction = new Transaction(
				fromAccount,
				systemAtmAccount,
				amount,
				AtmConstants.WITHDRAWAL_DESCRIPTION,
				customer);
		transactionRepository.save(transaction);

		return new AtmWithdrawResponse(
				transaction.getId(),
				fromAccount.getIban(),
				amount,
				fromAccount.getBalance());
	}

	private BankAccount resolveWithdrawalAccount(UserRegistration customer, String fromIban) {
		if (fromIban != null && !fromIban.isBlank()) {
			String normalizedIban = fromIban.trim().toUpperCase();
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
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active checking account found for withdrawal");
		}
		if (checkingAccounts.size() > 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Multiple checking accounts found; specify fromIban");
		}
		return checkingAccounts.getFirst();
	}

	private String formatAmount(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
	}
}

package com.example.backend.service;

import com.example.backend.domain.BankAccount;
import com.example.backend.domain.UserRegistration;
import com.example.backend.dto.UpdateCustomerLimitsRequest;
import com.example.backend.dto.UpdateCustomerLimitsResponse;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Business logic for updating absolute and daily customer transfer limits.
 * Changes apply immediately to every account the customer owns.
 */
@Service
public class CustomerLimitsService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final BankAccountRepository bankAccountRepository;

	public CustomerLimitsService(
			UserRegistrationRepository userRegistrationRepository,
			BankAccountRepository bankAccountRepository) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.bankAccountRepository = bankAccountRepository;
	}

	@Transactional // all account updates succeed together or all roll back
	public UpdateCustomerLimitsResponse updateCustomerLimits(
			Long customerId,
			UpdateCustomerLimitsRequest request) {

		// At least one field must be sent (employee can update one or both limits)
		if (request.absoluteLimit() == null && request.dailyOutgoingTransferLimit() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"At least one of absoluteLimit or dailyOutgoingTransferLimit is required");
		}

		UserRegistration customer = userRegistrationRepository.findById(customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

		// Employees cannot receive customer limits
		if (!"CUSTOMER".equals(customer.getRole())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limits can only be set for customers");
		}

		List<BankAccount> accounts = bankAccountRepository.findAllByOwner_Id(customerId);
		if (accounts.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer has no bank accounts");
		}

		// Round to 2 decimal places (euro cents)
		BigDecimal absoluteLimit = request.absoluteLimit() != null
				? request.absoluteLimit().setScale(2, RoundingMode.HALF_UP)
				: null;
		BigDecimal dailyOutgoingTransferLimit = request.dailyOutgoingTransferLimit() != null
				? request.dailyOutgoingTransferLimit().setScale(2, RoundingMode.HALF_UP)
				: null;

		for (BankAccount account : accounts) {
			if (absoluteLimit != null) {
				// Absolute limit is stored as minimumAllowedBalance (max balance cap in this project)
				// Cannot set cap below what the account already holds
				if (account.getBalance().compareTo(absoluteLimit) > 0) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"absoluteLimit cannot be lower than the current balance on account " + account.getIban());
				}
				account.setMinimumAllowedBalance(absoluteLimit);
			}
			if (dailyOutgoingTransferLimit != null) {
				// Daily transfer limit on this account
				account.setDailyOutgoingTransferLimit(dailyOutgoingTransferLimit);
			}
		}

		bankAccountRepository.saveAll(accounts);

		BankAccount firstAccount = accounts.getFirst();
		return new UpdateCustomerLimitsResponse(
				customerId,
				firstAccount.getMinimumAllowedBalance(),
				firstAccount.getDailyOutgoingTransferLimit(),
				accounts.size());
	}
}

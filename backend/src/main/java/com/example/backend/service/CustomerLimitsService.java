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

	@Transactional
	public UpdateCustomerLimitsResponse updateCustomerLimits(
			Long customerId,
			UpdateCustomerLimitsRequest request) {
		if (request.absoluteLimit() == null && request.dailyOutgoingTransferLimit() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"At least one of absoluteLimit or dailyOutgoingTransferLimit is required");
		}

		UserRegistration customer = userRegistrationRepository.findById(customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

		if (!"CUSTOMER".equals(customer.getRole())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limits can only be set for customers");
		}

		List<BankAccount> accounts = bankAccountRepository.findAllByOwner_Id(customerId);
		if (accounts.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer has no bank accounts");
		}

		BigDecimal absoluteLimit = request.absoluteLimit() != null
				? request.absoluteLimit().setScale(2, RoundingMode.HALF_UP)
				: null;
		BigDecimal dailyOutgoingTransferLimit = request.dailyOutgoingTransferLimit() != null
				? request.dailyOutgoingTransferLimit().setScale(2, RoundingMode.HALF_UP)
				: null;

		for (BankAccount account : accounts) {
			if (absoluteLimit != null) {
				if (account.getBalance().compareTo(absoluteLimit) > 0) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"absoluteLimit cannot be lower than the current balance on account " + account.getIban());
				}
				account.setMinimumAllowedBalance(absoluteLimit);
			}
			if (dailyOutgoingTransferLimit != null) {
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

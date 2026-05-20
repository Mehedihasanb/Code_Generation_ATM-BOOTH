package com.example.backend.policy;

import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.BankAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccountOpeningPolicy {

	private final CustomerRegistrationPolicy customerRegistrationPolicy;
	private final BankAccountRepository bankAccountRepository;

	public AccountOpeningPolicy(
		CustomerRegistrationPolicy customerRegistrationPolicy,
		BankAccountRepository bankAccountRepository
	) {
		this.customerRegistrationPolicy = customerRegistrationPolicy;
		this.bankAccountRepository = bankAccountRepository;
	}

	public void requireEligibleForNewAccounts(UserRegistration customer) {
		customerRegistrationPolicy.requirePendingApproval(customer);
		if (bankAccountRepository.existsByOwner_Id(customer.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer already has bank accounts");
		}
	}
}

package com.example.backend.policy;

import com.example.backend.domain.UserRegistration;
import com.example.backend.exception.BadRequestException;
import com.example.backend.repository.BankAccountRepository;
import org.springframework.stereotype.Component;

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
			throw new BadRequestException("Customer already has bank accounts");
		}
	}
}

package com.example.backend.policy;

import com.example.backend.domain.UserRegistration;
import com.example.backend.exception.BadRequestException;
import com.example.backend.repository.BankAccountRepository;
import org.springframework.stereotype.Component;

// US-10 rules before opening accounts - called from AccountService on approve
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

	// throws if customer cant get new accounts - service just calls this one method
	public void requireEligibleForNewAccounts(UserRegistration customer) {
		customerRegistrationPolicy.requirePendingApproval(customer); // must be PENDING customer
		if (bankAccountRepository.existsByOwner_Id(customer.getId())) {
			throw new BadRequestException("Customer already has bank accounts");
		}
	}
}

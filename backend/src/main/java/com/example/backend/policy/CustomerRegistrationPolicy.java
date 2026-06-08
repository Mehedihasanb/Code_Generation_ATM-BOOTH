package com.example.backend.policy;

import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.RegistrationDeniedException;
import org.springframework.stereotype.Component;

// business rules for customer registration status - keeps RegistrationService thin (SRP)
@Component
public class CustomerRegistrationPolicy {

	// shared check - this action is only for customers not employees
	public void requireCustomer(UserRegistration user) {
		if (!"CUSTOMER".equals(user.getRole())) {
			throw new BadRequestException("Only customers are supported for this action");
		}
	}

	// US-10 - employee can only open accounts if customer is still PENDING
	public void requirePendingApproval(UserRegistration customer) {
		requireCustomer(customer);
		if (customer.getCustomerApprovalStatus() != CustomerApprovalStatus.PENDING) {
			throw new BadRequestException("Customer registration must be pending before opening accounts");
		}
	}

	// deny on service desk - only works on PENDING, cant deny someone already approved
	public void requirePendingForDeny(UserRegistration customer) {
		requireCustomer(customer);
		if (customer.getCustomerApprovalStatus() != CustomerApprovalStatus.PENDING) {
			throw new BadRequestException("Only pending registrations can be denied");
		}
	}

	// US-05 login - eva case, denied customer cant login even with right password
	public void requireNotDeniedForLogin(UserRegistration user) {
		if ("CUSTOMER".equals(user.getRole())
			&& user.getCustomerApprovalStatus() == CustomerApprovalStatus.DENIED) {
			throw new RegistrationDeniedException("Registration was denied");
		}
	}
}

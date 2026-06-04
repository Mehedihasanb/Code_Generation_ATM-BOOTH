package com.example.backend.policy;

import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.RegistrationDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CustomerRegistrationPolicy {

	public void requireCustomer(UserRegistration user) {
		if (!"CUSTOMER".equals(user.getRole())) {
			throw new BadRequestException("Only customers are supported for this action");
		}
	}

	public void requirePendingApproval(UserRegistration customer) {
		requireCustomer(customer);
		if (customer.getCustomerApprovalStatus() != CustomerApprovalStatus.PENDING) {
			throw new BadRequestException("Customer registration must be pending before opening accounts");
		}
	}

	public void requirePendingForDeny(UserRegistration customer) {
		requireCustomer(customer);
		if (customer.getCustomerApprovalStatus() != CustomerApprovalStatus.PENDING) {
			throw new BadRequestException("Only pending registrations can be denied");
		}
	}

	public void requireNotDeniedForLogin(UserRegistration user) {
		if ("CUSTOMER".equals(user.getRole())
			&& user.getCustomerApprovalStatus() == CustomerApprovalStatus.DENIED) {
			throw new RegistrationDeniedException("Registration was denied");
		}
	}
}

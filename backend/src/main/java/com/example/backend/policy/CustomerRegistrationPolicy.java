package com.example.backend.policy;

import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CustomerRegistrationPolicy {

	public void requireCustomer(UserRegistration user) {
		if (!"CUSTOMER".equals(user.getRole())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only customers are supported for this action");
		}
	}

	public void requirePendingApproval(UserRegistration customer) {
		requireCustomer(customer);
		if (customer.getCustomerApprovalStatus() != CustomerApprovalStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Customer registration must be pending before opening accounts");
		}
	}

	public void requirePendingForDeny(UserRegistration customer) {
		requireCustomer(customer);
		if (customer.getCustomerApprovalStatus() != CustomerApprovalStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending registrations can be denied");
		}
	}

	public void requireNotDeniedForLogin(UserRegistration user) {
		if ("CUSTOMER".equals(user.getRole())
			&& user.getCustomerApprovalStatus() == CustomerApprovalStatus.DENIED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration was denied");
		}
	}
}

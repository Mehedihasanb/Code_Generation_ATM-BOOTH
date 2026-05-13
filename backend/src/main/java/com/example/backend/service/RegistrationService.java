package com.example.backend.service;

import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.web.dto.RegisterRequest;
import com.example.backend.web.dto.RegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final PasswordEncoder passwordEncoder;

	public RegistrationService(UserRegistrationRepository userRegistrationRepository, PasswordEncoder passwordEncoder) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public RegisterResponse register(RegisterRequest registerRequest) {
		if (userRegistrationRepository.findByEmail(registerRequest.email()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already registered");
		}

		UserRegistration newlyRegisteredUser = userRegistrationRepository.save(
			new UserRegistration(
				registerRequest.firstName().trim(),
				registerRequest.lastName().trim(),
				registerRequest.email().trim().toLowerCase(),
				passwordEncoder.encode(registerRequest.password()),
				"CUSTOMER",
				CustomerApprovalStatus.PENDING,
				registerRequest.bsnNumber().trim(),
				registerRequest.phoneNumber().trim(),
				null
			)
		);

		return new RegisterResponse(
			newlyRegisteredUser.getId(),
			newlyRegisteredUser.getFirstName(),
			newlyRegisteredUser.getLastName(),
			newlyRegisteredUser.getEmail(),
			"Registration successful"
		);
	}

	@Transactional
	public void denyCustomerRegistration(Long customerRegistrationId) {
		UserRegistration customer = userRegistrationRepository.findById(customerRegistrationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

		if (!"CUSTOMER".equals(customer.getRole())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only customer registrations can be denied");
		}

		if (customer.getCustomerApprovalStatus() != CustomerApprovalStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending registrations can be denied");
		}

		customer.setCustomerApprovalStatus(CustomerApprovalStatus.DENIED);
		userRegistrationRepository.save(customer);
	}
}

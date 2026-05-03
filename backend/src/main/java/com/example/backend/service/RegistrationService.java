package com.example.backend.service;

import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.web.dto.RegisterRequest;
import com.example.backend.web.dto.RegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final PasswordEncoder passwordEncoder;

	public RegistrationService(UserRegistrationRepository userRegistrationRepository, PasswordEncoder passwordEncoder) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public RegisterResponse register(RegisterRequest request) {
		if (userRegistrationRepository.findByEmail(request.email()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
		}

		UserRegistration created = userRegistrationRepository.save(
			new UserRegistration(
				request.firstName().trim(),
				request.lastName().trim(),
				request.email().trim().toLowerCase(),
				passwordEncoder.encode(request.password()),
				"CUSTOMER",
				false,
				request.bsnNumber().trim(),
				request.phoneNumber().trim()
			)
		);

		return new RegisterResponse(
			created.getId(),
			created.getFirstName(),
			created.getLastName(),
			created.getEmail(),
			"Registration successful"
		);
	}

	public void approveCustomer(Long id) {
		UserRegistration user = userRegistrationRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (!"CUSTOMER".equals(user.getRole())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only customers can be approved");
		}

		if (!user.isApproved()) {
			user.setApproved(true);
			userRegistrationRepository.save(user);
		}
	}
}
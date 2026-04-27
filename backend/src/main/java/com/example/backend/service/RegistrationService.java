package com.example.backend.service;

import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.web.dto.RegisterRequest;
import com.example.backend.web.dto.RegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

	private final UserRegistrationRepository userRegistrationRepository;

	public RegistrationService(UserRegistrationRepository userRegistrationRepository) {
		this.userRegistrationRepository = userRegistrationRepository;
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
				request.password()
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
}
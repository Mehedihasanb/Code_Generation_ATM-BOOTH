package com.example.backend.service;

import com.example.backend.config.JwtService;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.RegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;

@Service
public class RegistrationService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager; // ADDED

	// ADDED AuthenticationManager to the constructor
	public RegistrationService(UserRegistrationRepository userRegistrationRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			AuthenticationManager authenticationManager) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager; // ADDED
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
						null));

		return new RegisterResponse(
				newlyRegisteredUser.getId(),
				newlyRegisteredUser.getFirstName(),
				newlyRegisteredUser.getLastName(),
				newlyRegisteredUser.getEmail(),
				"Registration successful");
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

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest loginRequest) {
		// Authenticate credentials
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

		// Fetch User
		UserRegistration authenticatedUser = userRegistrationRepository
				.findByEmail(loginRequest.email().trim().toLowerCase())
				.orElseThrow(() -> new RuntimeException("User not found after authentication"));

		// Business Logic: Check if denied
		if ("CUSTOMER".equals(authenticatedUser.getRole())
				&& authenticatedUser.getCustomerApprovalStatus() == CustomerApprovalStatus.DENIED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration was denied");
		}

		// Generate Token
		UserDetails authenticatedUserDetails = (UserDetails) authentication.getPrincipal();
		String jsonWebToken = jwtService.generateToken(authenticatedUserDetails);

		// Build Response
		LoginResponse loginResponse = new LoginResponse();
		loginResponse.setToken(jsonWebToken);
		loginResponse.setRole(authenticatedUser.getRole());

		if ("CUSTOMER".equals(authenticatedUser.getRole()) && authenticatedUser.getCustomerApprovalStatus() != null) {
			loginResponse.setCustomerApprovalStatus(authenticatedUser.getCustomerApprovalStatus().name());
		} else {
			loginResponse.setCustomerApprovalStatus(null);
		}

		loginResponse.setEmployeeType(authenticatedUser.getEmployeeType());
		loginResponse.setFirstName(authenticatedUser.getFirstName());

		return loginResponse;
	}
}
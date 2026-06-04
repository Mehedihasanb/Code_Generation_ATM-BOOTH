package com.example.backend.service;

import com.example.backend.config.JwtService;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.policy.CustomerRegistrationPolicy;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.RegisterResponse;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;

@Service
public class RegistrationService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final CustomerRegistrationPolicy customerRegistrationPolicy;

	public RegistrationService(
		UserRegistrationRepository userRegistrationRepository,
		PasswordEncoder passwordEncoder,
		JwtService jwtService,
		AuthenticationManager authenticationManager,
		CustomerRegistrationPolicy customerRegistrationPolicy
	) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
		this.customerRegistrationPolicy = customerRegistrationPolicy;
	}

	public RegisterResponse register(RegisterRequest registerRequest) {
		if (userRegistrationRepository.findByEmail(registerRequest.email()).isPresent()) {
			throw new BadRequestException("Email is already registered");
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
						registerRequest.phoneNumber().trim()));

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
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

		customerRegistrationPolicy.requirePendingForDeny(customer);

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
				.orElseThrow(() -> new ResourceNotFoundException("User not found after authentication"));

		customerRegistrationPolicy.requireNotDeniedForLogin(authenticatedUser);

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

		loginResponse.setFirstName(authenticatedUser.getFirstName());

		return loginResponse;
	}
}
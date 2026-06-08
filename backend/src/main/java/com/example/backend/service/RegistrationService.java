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

// my auth service - register, login, deny. controller stays thin
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

	// someone signs up on /register
	public RegisterResponse register(RegisterRequest registerRequest) {
		// lowercase email so Dave@x and dave@x count as same
		var existingUser = userRegistrationRepository.findByEmail(registerRequest.email().trim().toLowerCase());
		if (existingUser.isPresent()) {
			// if employee already denied them (like eva) they cant register again
			if (existingUser.get().getCustomerApprovalStatus() == CustomerApprovalStatus.DENIED) {
				throw new BadRequestException(
						"This registration was denied. You cannot register again with this email.");
			}
			throw new BadRequestException("Email is already registered");
		}

		// save as CUSTOMER + PENDING, they cant bank yet till employee approves
		UserRegistration newlyRegisteredUser = userRegistrationRepository.save(
				new UserRegistration(
						registerRequest.firstName().trim(),
						registerRequest.lastName().trim(),
						registerRequest.email().trim().toLowerCase(),
						passwordEncoder.encode(registerRequest.password()), // hash it, dont store plain password
						"CUSTOMER",
						CustomerApprovalStatus.PENDING,
						registerRequest.bsnNumber().trim(),
						registerRequest.phoneNumber().trim()));

		// send back register response, no password in json
		return new RegisterResponse(
				newlyRegisteredUser.getId(),
				newlyRegisteredUser.getFirstName(),
				newlyRegisteredUser.getLastName(),
				newlyRegisteredUser.getEmail(),
				"Registration successful");
	}

	// employee hits deny on service desk
	@Transactional
	public void denyCustomerRegistration(Long customerRegistrationId) {
		UserRegistration customer = userRegistrationRepository.findById(customerRegistrationId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

		// only works if theyre still pending, not already approved
		customerRegistrationPolicy.requirePendingForDeny(customer);

		// flip status to denied so login + register both block them after this
		customer.setCustomerApprovalStatus(CustomerApprovalStatus.DENIED);
		userRegistrationRepository.save(customer);
	}

	// US-05 login - gives back jwt
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest loginRequest) {
		// spring checks password for me, wrong password = 401
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

		// grab user from db so i can send role + approval status to frontend
		UserRegistration authenticatedUser = userRegistrationRepository
				.findByEmail(loginRequest.email().trim().toLowerCase())
				.orElseThrow(() -> new ResourceNotFoundException("User not found after authentication"));

		// denied cant login even with right password (the eva case)
		customerRegistrationPolicy.requireNotDeniedForLogin(authenticatedUser);

		// pending + approved can login, vue decides where to send them
		UserDetails authenticatedUserDetails = (UserDetails) authentication.getPrincipal();
		String jsonWebToken = jwtService.generateToken(authenticatedUserDetails);

		// pack what frontend needs: token for later requests, role for nav, name for greeting
		LoginResponse loginResponse = new LoginResponse();
		loginResponse.setToken(jsonWebToken);
		loginResponse.setRole(authenticatedUser.getRole());

		// customer gets PENDING/APPROVED/DENIED, employee gets null here
		if ("CUSTOMER".equals(authenticatedUser.getRole()) && authenticatedUser.getCustomerApprovalStatus() != null) {
			loginResponse.setCustomerApprovalStatus(authenticatedUser.getCustomerApprovalStatus().name());
		} else {
			loginResponse.setCustomerApprovalStatus(null);
		}

		loginResponse.setFirstName(authenticatedUser.getFirstName()); // shows "Hello, Customer" in header

		return loginResponse;
	}
}

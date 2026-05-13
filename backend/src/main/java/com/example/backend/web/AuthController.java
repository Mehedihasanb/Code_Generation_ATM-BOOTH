package com.example.backend.web;

import com.example.backend.config.JwtService;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.service.RegistrationService;
import com.example.backend.web.dto.LoginRequest;
import com.example.backend.web.dto.LoginResponse;
import com.example.backend.web.dto.RegisterRequest;
import com.example.backend.web.dto.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authentication and self-service registration. Spring injects the collaborators below through the constructor
 * (constructor-based dependency injection): you do not create this controller with {@code new}; the framework supplies
 * {@link AuthenticationManager}, {@link JwtService}, etc., when the app starts.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserRegistrationRepository userRegistrationRepository;
	private final RegistrationService registrationService;

	public AuthController(
		AuthenticationManager authenticationManager,
		JwtService jwtService,
		UserRegistrationRepository userRegistrationRepository,
		RegistrationService registrationService
	) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userRegistrationRepository = userRegistrationRepository;
		this.registrationService = registrationService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a new customer (public)")
	public RegisterResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
		return registrationService.register(registerRequest);
	}

	@PostMapping("/login")
	@Operation(summary = "Login and receive a JWT")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
		);

		UserRegistration authenticatedUser = userRegistrationRepository
			.findByEmail(loginRequest.email().trim().toLowerCase())
			.orElseThrow(() -> new RuntimeException("User not found after authentication"));

		if ("CUSTOMER".equals(authenticatedUser.getRole())
			&& authenticatedUser.getCustomerApprovalStatus() == CustomerApprovalStatus.DENIED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration was denied");
		}

		UserDetails authenticatedUserDetails = (UserDetails) authentication.getPrincipal();
		String jsonWebToken = jwtService.generateToken(authenticatedUserDetails);

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

		return ResponseEntity.ok(loginResponse);
	}

	@PostMapping("/customers/{customerRegistrationId}/deny")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Deny a pending customer registration (employee only)")
	@SecurityRequirement(name = "bearerAuth")
	public void denyCustomerRegistration(@PathVariable Long customerRegistrationId) {
		registrationService.denyCustomerRegistration(customerRegistrationId);
	}
}

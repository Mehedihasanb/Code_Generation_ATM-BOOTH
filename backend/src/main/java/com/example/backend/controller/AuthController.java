package com.example.backend.controller;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.RegisterResponse;
import com.example.backend.service.RegistrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

	private final RegistrationService registrationService;

	public AuthController(RegistrationService registrationService) {
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
		// Pure routing. The service handles the heavy lifting.
		return ResponseEntity.ok(registrationService.login(loginRequest));
	}

	// Note: Technically this belongs in CustomerDirectoryController,
	// but I left it here to don't break Majd's routing!
	@PostMapping("/customers/{customerRegistrationId}/deny")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Deny a pending customer registration (employee only)")
	@SecurityRequirement(name = "bearerAuth")
	public void denyCustomerRegistration(@PathVariable Long customerRegistrationId) {
		registrationService.denyCustomerRegistration(customerRegistrationId);
	}
}
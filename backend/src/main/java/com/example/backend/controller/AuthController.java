package com.example.backend.controller;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.RegisterResponse;
import com.example.backend.service.RegistrationService;
import com.example.backend.service.UserDeletionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

	private final RegistrationService registrationService;
	private final UserDeletionService userDeletionService;

	public AuthController(RegistrationService registrationService, UserDeletionService userDeletionService) {
		this.registrationService = registrationService;
		this.userDeletionService = userDeletionService;
	}

	// no login needed, new customer fills the form and we save them as pending
	// they cant use banking till employee approves them
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a new customer (public)")
	public RegisterResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
		return registrationService.register(registerRequest);
	}

	// you dont need a token to call login
	// check email + password, if good send back jwt, frontend saves it for other requests
	@PostMapping("/login")
	@Operation(summary = "Login and receive a JWT")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
		return ResponseEntity.ok(registrationService.login(loginRequest));
	}

	// employee must be logged in with token
	// sets customer status to denied so they cant get approved later
	@PostMapping("/customers/{customerRegistrationId}/deny")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Deny a pending customer registration (employee only)")
	@SecurityRequirement(name = "bearerAuth")
	public void denyCustomerRegistration(@PathVariable Long customerRegistrationId) {
		registrationService.denyCustomerRegistration(customerRegistrationId);
	}

	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete your own account (soft by default, permanent when permanent=true)")
	@SecurityRequirement(name = "bearerAuth")
	// DELETE /auth/me - customer or employee deletes their own login
	// ?permanent=false = soft delete (deleted flag, accounts inactive, cant login)
	// ?permanent=true = hard delete (row gone, email free again, needs zero balance)
	public void deleteMyAccount(
			@RequestParam(defaultValue = "false") boolean permanent,
			Authentication authentication) {
		// null targetUserId tells UserDeletionService to delete the logged-in user
		userDeletionService.deleteAccount(authentication.getName(), null, permanent);
	}
}
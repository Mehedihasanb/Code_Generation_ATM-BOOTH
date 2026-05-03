package com.example.backend.web;

import com.example.backend.config.JwtService;
import com.example.backend.web.dto.LoginRequest;
import com.example.backend.web.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public login endpoint. Everything else stays locked until JwtAuthenticationFilter accepts a Bearer token.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	@Operation(summary = "Authenticate with email and password and receive a JWT")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		try {
			// Checks password using UserDetailsService + PasswordEncoder beans from SecurityConfig.
			Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email(), request.password())
			);

			UserDetails userDetails = (UserDetails) authentication.getPrincipal();
			// Client sends this token back as Authorization: Bearer ... on later requests.
			String token = jwtService.generateToken(userDetails);
			return new LoginResponse(token);
		} catch (BadCredentialsException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}
	}
}

package com.example.backend.service;

import com.example.backend.config.JwtService;
import com.example.backend.dto.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.backend.domain.UserRegistration;
import com.example.backend.dto.AuthRequest;
import com.example.backend.dto.RegisterResponse;
import com.example.backend.repository.UserRegistrationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public UserService(UserRegistrationRepository userRegistrationRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Transactional
	public RegisterResponse register(AuthRequest request) {
		if (userRegistrationRepository.findByEmail(request.getEmail()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already registered");
		}

		UserRegistration created = userRegistrationRepository.save(
				new UserRegistration(
						request.getFirstName() != null ? request.getFirstName().trim() : "",
						request.getLastName() != null ? request.getLastName().trim() : "",
						request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "",
						passwordEncoder.encode(request.getPassword()),
						"CUSTOMER",
						false, // By default, customers are NOT approved
						request.getBsnNumber() != null ? request.getBsnNumber().trim() : null,
						request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null));

		return new RegisterResponse(
				created.getId(),
				created.getFirstName(),
				created.getLastName(),
				created.getEmail(),
				"Registration successful");
	}

	@Transactional(readOnly = true)
	public LoginResponse login(AuthRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		UserDetails userDetails = (UserDetails) authentication.getPrincipal();

		// Use your existing findByEmail method
		UserRegistration user = findByEmail(request.getEmail());
		String token = jwtService.generateToken(userDetails);

		LoginResponse response = new LoginResponse();
		response.setToken(token);
		response.setRole(user.getRole());
		response.setApproved(user.isApproved());
		response.setFirstName(user.getFirstName());

		return response;
	}

	@Transactional(readOnly = true)
	public UserRegistration findByEmail(String email) {
		return userRegistrationRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	@Transactional(readOnly = true)
	public List<RegisterResponse> getPendingCustomers() {
		// 1. THIS IS THE MISSING LINE: Fetch the users from the database
		List<UserRegistration> pendingUsers = userRegistrationRepository.findByRoleAndApprovedFalse("CUSTOMER");

		// 2. Map the database entities into your existing RegisterResponse DTO
		return pendingUsers.stream()
				.map(user -> new RegisterResponse(
						user.getId(),
						user.getFirstName(),
						user.getLastName(),
						user.getEmail(),
						"Pending Approval" // Reusing the message field safely
				))
				.collect(Collectors.toList());
	}

	@Transactional
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

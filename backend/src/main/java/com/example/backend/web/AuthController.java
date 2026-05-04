package com.example.backend.web;

import com.example.backend.config.JwtService;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.domain.UserRegistration;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.*;


/**
 * Public login endpoint. Everything else stays locked until JwtAuthenticationFilter accepts a Bearer token.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRegistrationRepository userRepository;

    // Inject the repository so we can fetch the user details
    public AuthController(AuthenticationManager authenticationManager, 
                          JwtService jwtService, 
                          UserRegistrationRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        
        // 1. Authenticate credentials (throws exception if invalid)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Fetch the full user entity from the database using their email
        UserRegistration user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        // 3. Generate the JWT Token
        String token = jwtService.generateToken(authentication);

        // 4. Build the response and include the user's first name
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setRole(user.getRole());
        response.setApproved(user.isApproved());
        response.setEmployeeType(user.getEmployeeType());
        
        // NEW: Attach the first name here!
        response.setFirstName(user.getFirstName()); 

        return ResponseEntity.ok(response);
    }
}

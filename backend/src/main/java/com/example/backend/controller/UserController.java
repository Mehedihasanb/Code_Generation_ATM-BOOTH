package com.example.backend.controller;

import com.example.backend.dto.AuthRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.RegisterResponse;
import com.example.backend.service.UserService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping
@Tag(name = "User & Authentication", description = "Endpoints for user login, registration, and employee approvals")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Log in a user", description = "Authenticates credentials and returns a JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Validated(AuthRequest.Login.class) @RequestBody AuthRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @Operation(summary = "Register a new customer", description = "Creates a new customer account pending employee approval.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already in use")
    })
    @PostMapping("/api/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Validated(AuthRequest.Register.class) @RequestBody AuthRequest request) {
        return userService.register(request);
    }

    @Operation(summary = "Get pending customers", description = "Employee endpoint to fetch a list of customers waiting for approval.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    })
    @GetMapping("/api/registrations/pending")
    public ResponseEntity<List<RegisterResponse>> getPendingCustomers() {
        return ResponseEntity.ok(userService.getPendingCustomers());
    }

    @Operation(summary = "Approve a customer", description = "Employee endpoint to approve a pending customer registration.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Customer approved successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PatchMapping("/api/registrations/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveCustomer(@PathVariable Long id) {
        userService.approveCustomer(id);
    }
}
package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// registration form data, validated before creating the customer
public record RegisterRequest(
		@NotBlank(message = "First name is required") String firstName,
		@NotBlank(message = "Last name is required") String lastName,
		@Email(message = "Email must be valid") @NotBlank(message = "Email is required") String email,
		@Size(min = 8, message = "Password must be at least 8 characters") String password,
		@NotBlank(message = "BSN number is required") String bsnNumber,
		@NotBlank(message = "Phone number is required") String phoneNumber) {
}
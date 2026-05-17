package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for {@code POST /auth/login}: JSON body only, separate from the
 * {@code UserRegistration} entity.
 */
public record LoginRequest(
		@NotBlank String email,
		@NotBlank String password) {
}

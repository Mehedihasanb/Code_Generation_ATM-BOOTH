package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

// login request body, kept separate from the entity
public record LoginRequest(
		@NotBlank String email,
		@NotBlank String password) {
}

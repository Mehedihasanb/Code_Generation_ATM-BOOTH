package com.example.backend.dto;

public record RegisterResponse(
		Long id,
		String firstName,
		String lastName,
		String email,
		String message) {
}
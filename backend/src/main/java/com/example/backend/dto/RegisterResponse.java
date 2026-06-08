package com.example.backend.dto;

// 201 response after POST /auth/register (no password in JSON)
public record RegisterResponse(
		Long id,
		String firstName,
		String lastName,
		String email,
		String message) {
}
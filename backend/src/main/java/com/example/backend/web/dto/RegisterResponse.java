package com.example.backend.web.dto;

public record RegisterResponse(
	Long id,
	String firstName,
	String lastName,
	String email,
	String message
) {
}
package com.example.backend.dto;

public class RegisterResponse {
	private Long id;
	private String firstName;
	private String lastName;
	private String email;
	private String message;

	public RegisterResponse(Long id, String firstName, String lastName, String email, String message) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.message = message;
	}

	public Long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getMessage() {
		return message;
	}
}
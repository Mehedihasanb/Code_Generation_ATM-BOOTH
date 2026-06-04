package com.example.backend.exception;

// thrown when a denied customer tries to log in. handler turns this into a 403
public class RegistrationDeniedException extends RuntimeException {
	public RegistrationDeniedException(String message) {
		super(message);
	}
}

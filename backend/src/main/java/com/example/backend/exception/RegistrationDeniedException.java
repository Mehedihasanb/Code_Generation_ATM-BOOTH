package com.example.backend.exception;

// 403 - denied customer tries login with correct password (eva case)
// thrown from CustomerRegistrationPolicy.requireNotDeniedForLogin
public class RegistrationDeniedException extends RuntimeException {
	public RegistrationDeniedException(String message) {
		super(message);
	}
}

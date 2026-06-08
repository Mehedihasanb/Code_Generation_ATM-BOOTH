package com.example.backend.exception;

// 404 - customer, user or iban not in db
// used in RegistrationService + AccountService when findById/findByEmail/findByIban empty
public class ResourceNotFoundException extends RuntimeException {
	public ResourceNotFoundException(String message) {
		super(message);
	}
}

package com.example.backend.exception;

// thrown when a customer, account or IBAN does not exist. handler turns this into a 404
public class ResourceNotFoundException extends RuntimeException {
	public ResourceNotFoundException(String message) {
		super(message);
	}
}

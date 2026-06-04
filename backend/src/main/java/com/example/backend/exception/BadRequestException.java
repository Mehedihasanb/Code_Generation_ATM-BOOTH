package com.example.backend.exception;

// thrown when a request breaks a business rule (not pending, already has accounts, etc). handler turns this into a 400
public class BadRequestException extends RuntimeException {
	public BadRequestException(String message) {
		super(message);
	}
}

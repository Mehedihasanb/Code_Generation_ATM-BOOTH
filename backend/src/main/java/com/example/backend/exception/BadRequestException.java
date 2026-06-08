package com.example.backend.exception;

// 400 - business rule broken (duplicate email, not pending, already has accounts)
// throw from service/policy, GlobalExceptionHandler sends message json to vue
public class BadRequestException extends RuntimeException {
	public BadRequestException(String message) {
		super(message); // this string ends up in ApiErrorResponse.message
	}
}

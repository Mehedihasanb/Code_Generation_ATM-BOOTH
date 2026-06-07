package com.example.backend.exception;

import com.example.backend.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "Not Found",
				ex.getMessage(), request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
				"Bad Request", ex.getMessage(), request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(RegistrationDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleDenied(RegistrationDeniedException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.FORBIDDEN.value(), "Forbidden",
				ex.getMessage(), request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
	}

	// For transactions and validation

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex,
			HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), ex.getStatusCode().value(),
				ex.getStatusCode().toString(), ex.getReason(), request.getRequestURI());
		return new ResponseEntity<>(response, ex.getStatusCode());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		// Combine all DTO @Valid errors into a readable string
		String errorMessage = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage())
				.collect(Collectors.joining("; "));

		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
				"Validation Error", errorMessage, request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	// Fallback for unexpected server crashes
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleAllOtherExceptions(Exception ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"Internal Server Error", "An unexpected system error occurred.", request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
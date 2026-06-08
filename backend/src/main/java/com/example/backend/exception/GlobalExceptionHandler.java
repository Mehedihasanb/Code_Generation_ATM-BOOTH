package com.example.backend.exception;

import com.example.backend.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

// catches thrown exceptions app-wide and turns them into same json error shape for frontend
@RestControllerAdvice
public class GlobalExceptionHandler {

	// my 404 - customer not found, unknown iban
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "Not Found",
				ex.getMessage(), request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	// my 400 - duplicate email, not pending, denied re-register etc
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
				"Bad Request", ex.getMessage(), request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	// wrong password on login
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(),
				"Unauthorized", "Invalid email or password", request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
	}

	// my 403 - eva cant login after deny
	@ExceptionHandler(RegistrationDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleDenied(RegistrationDeniedException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.FORBIDDEN.value(), "Forbidden",
				ex.getMessage(), request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
	}

	// teammates use ResponseStatusException in TransactionService etc - same json format
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex,
			HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), ex.getStatusCode().value(),
				ex.getStatusCode().toString(), ex.getReason(), request.getRequestURI());
		return new ResponseEntity<>(response, ex.getStatusCode());
	}

	// @Valid on dtos failed - e.g. blank email on register, joins all field errors into one message
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String errorMessage = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage())
				.collect(Collectors.joining("; "));

		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
				"Validation Error", errorMessage, request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	// anything we didnt expect - dont leak stack trace to client
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleAllOtherExceptions(Exception ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"Internal Server Error", "An unexpected system error occurred.", request.getRequestURI());
		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

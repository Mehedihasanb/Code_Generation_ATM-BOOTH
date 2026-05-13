package com.example.backend.web.dto;

/**
 * JSON body returned by {@code POST /auth/login}. This is a DTO: it is not a JPA entity and maps what the client needs
 * (token + profile fields) without exposing the internal {@code UserRegistration} entity shape.
 */
public class LoginResponse {
	private String token;
	private String role;
	/** For customers: PENDING, APPROVED, or DENIED. Null for employees. */
	private String customerApprovalStatus;
	private String employeeType;
	private String firstName;

	public LoginResponse() {
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getCustomerApprovalStatus() {
		return customerApprovalStatus;
	}

	public void setCustomerApprovalStatus(String customerApprovalStatus) {
		this.customerApprovalStatus = customerApprovalStatus;
	}

	public String getEmployeeType() {
		return employeeType;
	}

	public void setEmployeeType(String employeeType) {
		this.employeeType = employeeType;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
}

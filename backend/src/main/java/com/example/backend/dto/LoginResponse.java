package com.example.backend.dto;

// login response sent back to the client (token + basic profile), not the entity
public class LoginResponse {
	private String token;
	private String role;
	// for customers: PENDING, APPROVED or DENIED. null for employees
	private String customerApprovalStatus;
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

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
}

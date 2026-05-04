package com.example.backend.dto;

public class LoginResponse {
    private String token;
    private String role;
    private boolean approved;
    private String employeeType;
    
    // NEW: Add the firstName field
    private String firstName; 

    public LoginResponse() {}

    // Update your getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getEmployeeType() { return employeeType; }
    public void setEmployeeType(String employeeType) { this.employeeType = employeeType; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
}
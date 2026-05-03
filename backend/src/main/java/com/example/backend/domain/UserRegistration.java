package com.example.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_registrations")
public class UserRegistration {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "FIRST_NAME")
	private String firstName;

	@Column(nullable = false)
	private String lastName;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String password;


	@Column(nullable = false)
	private String role;

	// Only for employees: "REGULAR" or "SERVICE_DESK". Null for customers.
	@Column(nullable = true)
	private String employeeType;

	@Column(nullable = false)
	private boolean approved;

	@Column(nullable = true)
	private String bsnNumber;

	@Column(nullable = true)
	private String phoneNumber;

	   public UserRegistration() {
	   }

	   public UserRegistration(String firstName, String lastName, String email, String password, String role) {
		   this(firstName, lastName, email, password, role, false, null, null, null);
	   }

	   public UserRegistration(String firstName, String lastName, String email, String password, String role, boolean approved, String bsnNumber, String phoneNumber) {
		   this(firstName, lastName, email, password, role, approved, bsnNumber, phoneNumber, null);
	   }

	   public UserRegistration(String firstName, String lastName, String email, String password, String role, boolean approved, String bsnNumber, String phoneNumber, String employeeType) {
		   this.firstName = firstName;
		   this.lastName = lastName;
		   this.email = email;
		   this.password = password;
		   this.role = role;
		   this.approved = approved;
		   this.bsnNumber = bsnNumber;
		   this.phoneNumber = phoneNumber;
		   this.employeeType = employeeType;
	   }

	public Long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}


	   public String getRole() {
		   return role;
	   }

	   public String getEmployeeType() {
		   return employeeType;
	   }

	public boolean isApproved() {
		return approved;
	}

	public String getBsnNumber() {
		return bsnNumber;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setPassword(String password) {
		this.password = password;
	}


	   public void setRole(String role) {
		   this.role = role;
	   }

	   public void setEmployeeType(String employeeType) {
		   this.employeeType = employeeType;
	   }

	public void setApproved(boolean approved) {
		this.approved = approved;
	}

	public void setBsnNumber(String bsnNumber) {
		this.bsnNumber = bsnNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
}
package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank(message = "Email is required", groups = {
                Login.class, Register.class }) @Email(message = "Email must be valid", groups = { Login.class,
                        Register.class }) String email,

        @NotBlank(message = "Password is required", groups = { Login.class,
                Register.class }) @Size(min = 8, message = "Password must be at least 8 characters", groups = Register.class) String password,

        @NotBlank(message = "First name is required", groups = Register.class) String firstName,

        @NotBlank(message = "Last name is required", groups = Register.class) String lastName,

        @NotBlank(message = "BSN number is required", groups = Register.class) String bsnNumber,

        @NotBlank(message = "Phone number is required", groups = Register.class) String phoneNumber) {

    public interface Login {
    }

    public interface Register {
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getBsnNumber() {
        return bsnNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
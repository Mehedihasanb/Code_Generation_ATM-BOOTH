package com.example.backend.repository;

import com.example.backend.domain.UserRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRegistrationRepository extends JpaRepository<UserRegistration, Long> {
	Optional<UserRegistration> findByEmail(String email);
}
package com.example.backend.repository;

import com.example.backend.domain.UserRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import java.util.Optional;

public interface UserRegistrationRepository extends JpaRepository<UserRegistration, Long> {

	Optional<UserRegistration> findByEmail(String email);

	// US-12: all customers (Spring builds the query from the method name)
	Page<UserRegistration> findByRole(String role, Pageable pageable);

	// US-09: pending customers waiting for employee approval (no accounts yet)
	@Query("SELECT u FROM UserRegistration u WHERE u.role = 'CUSTOMER' AND u.customerApprovalStatus = 'PENDING' AND NOT EXISTS (SELECT 1 FROM BankAccount a WHERE a.owner.id = u.id)")
	Page<UserRegistration> findCustomersWithoutAccounts(Pageable pageable);

	List<UserRegistration> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
}

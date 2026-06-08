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
	Optional<UserRegistration> findByEmailAndDeletedFalse(String email); // login path - skip soft-deleted
	Page<UserRegistration> findByRoleAndDeletedFalse(String role, Pageable pageable); // directory hides deleted
	Page<UserRegistration> findByRoleAndDeletedTrue(String role, Pageable pageable); // deactivated list for employee

	long countByRoleAndDeletedFalse(String role); // UserDeletionPolicy - count active employees

	@Query("SELECT u FROM UserRegistration u WHERE u.role = 'CUSTOMER' AND u.deleted = false AND u.customerApprovalStatus = 'PENDING' AND NOT EXISTS (SELECT 1 FROM BankAccount a WHERE a.owner.id = u.id)")
	Page<UserRegistration> findCustomersWithoutAccounts(Pageable pageable); // service desk list, no deleted
	List<UserRegistration> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDeletedFalse(String firstName, String lastName);
	List<UserRegistration> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDeletedTrue(String firstName, String lastName);

}



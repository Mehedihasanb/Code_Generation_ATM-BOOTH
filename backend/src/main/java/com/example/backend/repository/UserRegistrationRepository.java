package com.example.backend.repository;

import com.example.backend.domain.UserRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRegistrationRepository extends JpaRepository<UserRegistration, Long> {
	Optional<UserRegistration> findByEmail(String email);

	@Query(
		"""
		select registration from UserRegistration registration
		where registration.role = 'CUSTOMER'
		and not exists (
			select 1 from BankAccount account where account.owner.id = registration.id
		)
		"""
	)
	Page<UserRegistration> findCustomersWhoHaveNoBankAccounts(Pageable pageable);

	@Query("select registration from UserRegistration registration where registration.role = 'CUSTOMER'")
	Page<UserRegistration> findAllCustomers(Pageable pageable);
}
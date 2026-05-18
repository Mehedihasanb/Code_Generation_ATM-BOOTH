package com.example.backend.repository;

import com.example.backend.domain.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

	Optional<BankAccount> findByIban(String iban);

	boolean existsByOwner_Id(Long ownerId);

	List<BankAccount> findByOwner_IdIn(List<Long> ownerIds);
}

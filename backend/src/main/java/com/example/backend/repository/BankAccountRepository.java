package com.example.backend.repository;

import com.example.backend.domain.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

	Optional<BankAccount> findByIban(String iban);

	List<BankAccount> findByOwner_IdIn(Collection<Long> ownerIds);

	List<BankAccount> findAllByOwner_Id(Long ownerId);

	boolean existsByOwner_Id(Long ownerId);

}
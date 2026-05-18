package com.example.backend.repository;

import com.example.backend.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.iban = :iban OR t.toAccount.iban = :iban ORDER BY t.timestamp DESC")
    Page<Transaction> findAllByIban(@Param("iban") String iban, Pageable pageable);
}
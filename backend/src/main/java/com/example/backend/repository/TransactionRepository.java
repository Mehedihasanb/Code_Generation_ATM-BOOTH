package com.example.backend.repository;

import com.example.backend.domain.Transaction;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.iban = :iban OR t.toAccount.iban = :iban ORDER BY t.timestamp DESC")
    Page<Transaction> findAllByIban(@Param("iban") String iban, Pageable pageable);

    // COALESCE ensures that if there are no transactions today, it returns 0
    // instead of null
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.fromAccount = :account AND t.timestamp >= :startOfDay")
    BigDecimal sumOutgoingTransactionsToday(
            @Param("account") com.example.backend.domain.BankAccount account,
            @Param("startOfDay") java.time.LocalDateTime startOfDay);

    Page<Transaction> findByFromAccount_IbanOrToAccount_IbanOrderByTimestampDesc(
            String fromIban,
            String toIban,
            Pageable pageable);
}
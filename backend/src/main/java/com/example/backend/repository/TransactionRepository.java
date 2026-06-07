package com.example.backend.repository;

import com.example.backend.domain.Transaction;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

        @Query("SELECT t FROM Transaction t WHERE t.fromAccount.iban = :iban OR t.toAccount.iban = :iban ORDER BY t.timestamp DESC")
        Page<Transaction> findAllByIban(@Param("iban") String iban, Pageable pageable);

        // COALESCE ensures that if there are no transactions today, it returns 0
        // instead of null
        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.fromAccount = :account AND t.timestamp >= :startOfDay")
        BigDecimal sumOutgoingTransactionsToday(
                        @Param("account") com.example.backend.domain.BankAccount account,
                        @Param("startOfDay") java.time.LocalDateTime startOfDay);

        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.toAccount = :account AND t.timestamp >= :startOfDay")
        BigDecimal sumIncomingTransactionsToday(
                        @Param("account") com.example.backend.domain.BankAccount account,
                        @Param("startOfDay") java.time.LocalDateTime startOfDay);

        Page<Transaction> findByFromAccount_IbanOrToAccount_IbanOrderByTimestampDesc(
                        String fromIban,
                        String toIban,
                        Pageable pageable);

        @Query("SELECT t FROM Transaction t WHERE " +
                        "((t.fromAccount.iban = :myIban AND (:counterpart IS NULL OR t.toAccount.iban = :counterpart)) OR "
                        +
                        " (t.toAccount.iban = :myIban AND (:counterpart IS NULL OR t.fromAccount.iban = :counterpart))) AND "
                        +
                        "(:startDate IS NULL OR t.timestamp >= :startDate) AND " +
                        "(:endDate IS NULL OR t.timestamp <= :endDate) AND " +
                        "(:exactAmount IS NULL OR t.amount = :exactAmount) AND " +
                        "(:minAmount IS NULL OR t.amount > :minAmount) AND " +
                        "(:maxAmount IS NULL OR t.amount < :maxAmount)")
        Page<Transaction> findWithFilters(
                        @Param("myIban") String myIban,
                        @Param("counterpart") String counterpart,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("exactAmount") BigDecimal exactAmount,
                        @Param("minAmount") BigDecimal minAmount,
                        @Param("maxAmount") BigDecimal maxAmount,
                        Pageable pageable);

        // Transactions where the user is the owner of sender/receiver account
        @Query("SELECT t FROM Transaction t WHERE " +
                        "(t.fromAccount IS NOT NULL AND t.fromAccount.owner.id = :userId) OR " +
                        "(t.toAccount IS NOT NULL AND t.toAccount.owner.id = :userId) " +
                        "ORDER BY t.timestamp DESC")
        Page<Transaction> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

        // Global Transaction History filter
        @Query("SELECT t FROM Transaction t WHERE " +
                        "(:targetIban IS NULL OR t.fromAccount.iban = :targetIban OR t.toAccount.iban = :targetIban) AND "
                        +
                        "(:startDate IS NULL OR t.timestamp >= :startDate) AND " +
                        "(:endDate IS NULL OR t.timestamp <= :endDate) AND " +
                        "(:exactAmount IS NULL OR t.amount = :exactAmount) AND " +
                        "(:minAmount IS NULL OR t.amount > :minAmount) AND " +
                        "(:maxAmount IS NULL OR t.amount < :maxAmount)")
        Page<Transaction> findSystemWithFilters(
                        @Param("targetIban") String targetIban,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("exactAmount") BigDecimal exactAmount,
                        @Param("minAmount") BigDecimal minAmount,
                        @Param("maxAmount") BigDecimal maxAmount,
                        Pageable pageable);
}
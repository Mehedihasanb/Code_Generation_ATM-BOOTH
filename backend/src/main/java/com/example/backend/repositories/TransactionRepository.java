package com.example.backend.repositories;

import com.example.backend.entities.Transaction;
import com.example.backend.entities.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer>,
        JpaSpecificationExecutor<Transaction> {

    // Used by TransactionService to compute daily outgoing total before calling TransactionRules
    List<Transaction> findByFromIbanAndTypeInAndTimestampGreaterThanEqual(
            String iban, Collection<TransactionType> types, LocalDateTime since);
}

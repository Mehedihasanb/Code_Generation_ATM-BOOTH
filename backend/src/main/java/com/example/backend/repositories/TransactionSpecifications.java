package com.example.backend.repositories;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import com.example.backend.dtos.TransactionFilterParams;
import com.example.backend.entities.Account;
import com.example.backend.entities.Transaction;
import com.example.backend.entities.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionSpecifications {

    private TransactionSpecifications() {}

    public static Specification<Transaction> fromFilters(TransactionFilterParams filters) {

        Specification<Transaction> spec = (root, q, cb) -> cb.conjunction();
        spec = andIfPresent(spec, withIban(filters.getIban()));
        spec = andIfPresent(spec, involvingAccount(filters.getAccountIban()));
        spec = andIfPresent(spec, involvingAccount(filters.getCounterpartIban()));
        spec = andIfPresent(spec, withType(filters.getType()));
        spec = andIfPresent(spec, withMinAmount(filters.getMinAmount()));
        spec = andIfPresent(spec, withMaxAmount(filters.getMaxAmount()));
        spec = andIfPresent(spec, withAmount(filters.getAmount(), filters.getAmountOperator()));
        spec = andIfPresent(spec, visibleToCustomer(filters.getCustomerId()));
        spec = andIfPresent(spec, withStartDate(filters.getStartDate()));
        spec = andIfPresent(spec, withEndDate(filters.getEndDate()));
        return spec;
    }

    private static Specification<Transaction> andIfPresent(
            Specification<Transaction> base, Specification<Transaction> clause) {
        return clause == null ? base : base.and(clause);
    }

    private static Specification<Transaction> withIban(String iban) {
        if (iban == null || iban.isBlank()) return null;
        return (root, q, cb) -> {
            String pattern = "%" + iban.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fromIban")), pattern),
                    cb.like(cb.lower(root.get("toIban")), pattern));
        };
    }

    private static Specification<Transaction> involvingAccount(String iban) {
        if (iban == null || iban.isBlank()) return null;
        return (root, q, cb) -> cb.or(
                cb.equal(root.get("fromIban"), iban),
                cb.equal(root.get("toIban"), iban));
    }

    private static Specification<Transaction> withType(TransactionType type) {
        if (type == null) return null;
        return (root, q, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<Transaction> withMinAmount(BigDecimal min) {
        if (min == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    private static Specification<Transaction> withMaxAmount(BigDecimal max) {
        if (max == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("amount"), max);
    }

    private static Specification<Transaction> withAmount(BigDecimal amount, String operator) {
        if (amount == null) return null;
        String op = operator == null || operator.isBlank() ? "eq" : operator;
        return switch (op) {
            case "gt" -> (root, q, cb) -> cb.greaterThan(root.get("amount"), amount);
            case "lt" -> (root, q, cb) -> cb.lessThan(root.get("amount"), amount);
            default -> (root, q, cb) -> cb.equal(root.get("amount"), amount);
        };
    }

    private static Specification<Transaction> visibleToCustomer(Integer customerId) {
        if (customerId == null) return null;
        return (root, query, cb) -> {
            Subquery<String> ownedIbans = query.subquery(String.class);
            Root<Account> acc = ownedIbans.from(Account.class);
            ownedIbans.select(acc.get("iban"))
                    .where(cb.equal(acc.get("user").get("id"), customerId));
            return cb.or(
                    cb.equal(root.get("initiatedBy").get("id"), customerId),
                    root.get("fromIban").in(ownedIbans),
                    root.get("toIban").in(ownedIbans));
        };
    }

    private static Specification<Transaction> withStartDate(java.time.LocalDate startDate) {
        if (startDate == null) return null;
        LocalDateTime start = startDate.atStartOfDay();
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), start);
    }

    private static Specification<Transaction> withEndDate(java.time.LocalDate endDate) {
        if (endDate == null) return null;
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        return (root, q, cb) -> cb.lessThan(root.get("timestamp"), endExclusive);
    }
}

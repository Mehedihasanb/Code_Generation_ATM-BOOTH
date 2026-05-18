package com.example.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private BankAccount fromAccount;

    @ManyToOne(optional = false)
    private BankAccount toAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    private String description;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(optional = false)
    private UserRegistration initiatingUser;

    // JPA requires a no-args constructor
    protected Transaction() {
    }

    public Transaction(BankAccount fromAccount, BankAccount toAccount, BigDecimal amount, String description,
            UserRegistration initiatingUser) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.initiatingUser = initiatingUser;
    }

    // --- Getters ---
    public Long getId() {
        return id;
    }

    public BankAccount getFromAccount() {
        return fromAccount;
    }

    public BankAccount getToAccount() {
        return toAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public UserRegistration getInitiatingUser() {
        return initiatingUser;
    }
}
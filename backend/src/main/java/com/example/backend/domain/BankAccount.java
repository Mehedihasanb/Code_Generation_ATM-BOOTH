package com.example.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

// bank account table, each customer can have checking and savings
@Entity
@Table(
	name = "bank_accounts",
	uniqueConstraints = @UniqueConstraint(name = "uk_bank_account_iban", columnNames = "iban")
)
public class BankAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// which customer owns this account
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private UserRegistration owner;

	@Column(nullable = false, length = 34)
	private String iban;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountType accountType;

	@Column(nullable = false)
	private boolean active; // false when employee closes the account

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal balance;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal minimumAllowedBalance; // max balance cap (absolute limit), set when employee approves

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal dailyOutgoingTransferLimit; // max outgoing per day

	public BankAccount() {
	}

	public BankAccount(
		UserRegistration owner,
		String iban,
		AccountType accountType,
		boolean active,
		BigDecimal balance,
		BigDecimal minimumAllowedBalance,
		BigDecimal dailyOutgoingTransferLimit
	) {
		this.owner = owner;
		this.iban = iban;
		this.accountType = accountType;
		this.active = active;
		this.balance = balance;
		this.minimumAllowedBalance = minimumAllowedBalance;
		this.dailyOutgoingTransferLimit = dailyOutgoingTransferLimit;
	}

	public Long getId() {
		return id;
	}

	public UserRegistration getOwner() {
		return owner;
	}

	public void setOwner(UserRegistration owner) {
		this.owner = owner;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public BigDecimal getMinimumAllowedBalance() {
		return minimumAllowedBalance;
	}

	public void setMinimumAllowedBalance(BigDecimal minimumAllowedBalance) {
		this.minimumAllowedBalance = minimumAllowedBalance;
	}

	public BigDecimal getDailyOutgoingTransferLimit() {
		return dailyOutgoingTransferLimit;
	}

	public void setDailyOutgoingTransferLimit(BigDecimal dailyOutgoingTransferLimit) {
		this.dailyOutgoingTransferLimit = dailyOutgoingTransferLimit;
	}
}

package com.example.backend.dto;

import com.example.backend.domain.BankAccount;

import java.math.BigDecimal;

// nested account info inside CustomerDirectoryRow for employee directory
public record CustomerAccountRow(
	String iban,
	String accountType,
	boolean active,
	BigDecimal balance,
	BigDecimal minimumAllowedBalance,
	BigDecimal dailyOutgoingTransferLimit) {

	public static CustomerAccountRow fromBankAccount(BankAccount account) {
		return new CustomerAccountRow(
			account.getIban(),
			account.getAccountType().name(),
			account.isActive(),
			account.getBalance(),
			account.getMinimumAllowedBalance(),
			account.getDailyOutgoingTransferLimit());
	}
}

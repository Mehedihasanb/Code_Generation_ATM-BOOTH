package com.example.backend.dto;

import com.example.backend.domain.BankAccount;

import java.math.BigDecimal;

public record AccountDetail(
	String iban,
	String accountType,
	BigDecimal balance,
	BigDecimal absoluteLimit) {

	public static AccountDetail fromBankAccount(BankAccount account) {
		return new AccountDetail(
			account.getIban(),
			account.getAccountType().name(),
			account.getBalance(),
			account.getMinimumAllowedBalance());
	}
}

package com.example.backend.dto;

import com.example.backend.domain.BankAccount;
import com.example.backend.domain.UserRegistration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// GET /accounts/me: safe view of customer name, total balance and account list (no password etc.)
public record AccountSummaryResponse(
	String customerName,
	BigDecimal combinedBalance,
	List<AccountDetail> accounts) {

	public static AccountSummaryResponse fromCustomerAndAccounts(
		UserRegistration customer,
		List<BankAccount> bankAccounts
	) {
		BigDecimal combinedBalance = BigDecimal.ZERO;
		List<AccountDetail> accountDetails = new ArrayList<>();
		for (BankAccount account : bankAccounts) {
			combinedBalance = combinedBalance.add(account.getBalance());
			accountDetails.add(AccountDetail.fromBankAccount(account));
		}
		String customerName = customer.getFirstName() + " " + customer.getLastName();
		return new AccountSummaryResponse(customerName, combinedBalance, accountDetails);
	}
}

package com.example.backend.support;

/**
 * Shared ATM values used by withdraw, deposit, and transaction history.
 */
public final class AtmConstants {

	// Virtual bank account that holds ATM cash; other side of every ATM transaction
	public static final String SYSTEM_ATM_IBAN = "NL00RHIN0000000001";
	public static final String WITHDRAWAL_DESCRIPTION = "ATM withdrawal";
	public static final String DEPOSIT_DESCRIPTION = "ATM deposit";

	private AtmConstants() {
	}
}

package com.example.backend.service;

import com.example.backend.repository.BankAccountRepository;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

// generates valid NL ibans when employee approves - AccountService calls this twice (checking + savings)
@Service
public class IbanAllocationService {

	private static final String DEMO_BANK_CODE = "INHO"; // fake bank code for our school project

	private final BankAccountRepository bankAccountRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public IbanAllocationService(BankAccountRepository bankAccountRepository) {
		this.bankAccountRepository = bankAccountRepository;
	}

	// iban4j external lib in pom.xml - builds real format NL iban, we just pick random account digits
	public String allocateUniqueDutchIban() {
		for (int attempt = 0; attempt < 100; attempt++) {
			// random 10 digit account number, padded with zeros if needed
			long accountDigits = Math.floorMod(secureRandom.nextLong(), 10_000_000_000L);
			String accountNumber = String.format("%010d", accountDigits);
			Iban iban = new Iban.Builder()
				.countryCode(CountryCode.NL)
				.bankCode(DEMO_BANK_CODE)
				.accountNumber(accountNumber)
				.build(); // library calculates check digits for us
			String candidate = iban.toString();
			if (bankAccountRepository.findByIban(candidate).isEmpty()) {
				return candidate; // not in db yet, good to use
			}
			// collision rare but if it happens try again
		}
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate a unique IBAN");
	}
}

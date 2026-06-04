package com.example.backend.service;

import com.example.backend.repository.BankAccountRepository;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

// uses the external iban4j library (see pom.xml) to build a valid Dutch IBAN with
// correct check digits, instead of writing the IBAN algorithm by hand
@Service
public class IbanAllocationService {

	private static final String DEMO_BANK_CODE = "INHO";

	private final BankAccountRepository bankAccountRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public IbanAllocationService(BankAccountRepository bankAccountRepository) {
		this.bankAccountRepository = bankAccountRepository;
	}

	public String allocateUniqueDutchIban() {
		for (int attempt = 0; attempt < 100; attempt++) {
			// Math.floorMod keeps the random number positive and within 10 digits (0..9999999999)
			long accountDigits = Math.floorMod(secureRandom.nextLong(), 10_000_000_000L);
			String accountNumber = String.format("%010d", accountDigits);
			Iban iban = new Iban.Builder()
				.countryCode(CountryCode.NL)
				.bankCode(DEMO_BANK_CODE)
				.accountNumber(accountNumber)
				.build();
			String candidate = iban.toString();
			if (bankAccountRepository.findByIban(candidate).isEmpty()) {
				return candidate;
			}
		}
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate a unique IBAN");
	}
}

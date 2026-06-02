package com.example.backend.config;

import com.example.backend.domain.AccountType;
import com.example.backend.domain.BankAccount;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.support.AtmConstants;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class AtmSystemAccountInitializer {

	@Bean
	CommandLineRunner seedAtmSystemAccount(
			BankAccountRepository bankAccountRepository,
			UserRegistrationRepository userRegistrationRepository) {
		return args -> {
			if (bankAccountRepository.findByIban(AtmConstants.SYSTEM_ATM_IBAN).isPresent()) {
				return;
			}
			userRegistrationRepository.findByEmail("employee@inholland.nl").ifPresent(employee -> {
				BankAccount systemAtmAccount = new BankAccount(
						employee,
						AtmConstants.SYSTEM_ATM_IBAN,
						AccountType.CHECKING,
						true,
						BigDecimal.ZERO.setScale(2),
						new BigDecimal("999999999.00"),
						new BigDecimal("999999999.00"));
				bankAccountRepository.save(systemAtmAccount);
			});
		};
	}
}

package com.example.backend.config;

import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

	@Bean
	CommandLineRunner seedUsers(UserRegistrationRepository userRegistrationRepository, PasswordEncoder passwordEncoder) {
		return startupArguments -> {
			if (userRegistrationRepository.findByEmail("customer@inholland.nl").isEmpty()) {
				userRegistrationRepository.save(new UserRegistration(
					"Customer",
					"User",
					"customer@inholland.nl",
					passwordEncoder.encode("Password123!"),
					"CUSTOMER",
					CustomerApprovalStatus.APPROVED,
					"123456789",
					"+31 6 12345678",
					null
				));
			}

			if (userRegistrationRepository.findByEmail("employee@inholland.nl").isEmpty()) {
				userRegistrationRepository.save(new UserRegistration(
					"Employee",
					"User",
					"employee@inholland.nl",
					passwordEncoder.encode("Password123!"),
					"EMPLOYEE",
					null,
					null,
					null,
					"REGULAR"
				));
			}

			if (userRegistrationRepository.findByEmail("servicedesk@inholland.nl").isEmpty()) {
				userRegistrationRepository.save(new UserRegistration(
					"ServiceDesk",
					"User",
					"servicedesk@inholland.nl",
					passwordEncoder.encode("Password123!"),
					"EMPLOYEE",
					null,
					null,
					null,
					"SERVICE_DESK"
				));
			}
		};
	}
}

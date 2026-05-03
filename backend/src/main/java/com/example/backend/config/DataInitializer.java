package com.example.backend.config;

import com.example.backend.domain.Message;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

	@Bean
	CommandLineRunner seedMessages(MessageRepository messages) {
		return args -> {
			if (messages.count() == 0) {
				messages.save(new Message("Hello from Spring Boot + H2 + JPA"));
			}
		};
	}

	@Bean
	CommandLineRunner seedUsers(UserRegistrationRepository registrations, PasswordEncoder passwordEncoder) {
		return args -> {
			if (registrations.findByEmail("customer@inholland.nl").isEmpty()) {
				registrations.save(new UserRegistration(
					"Customer",
					"User",
					"customer@inholland.nl",
					passwordEncoder.encode("Password123!"),
					"CUSTOMER",
					true,
					"123456789",
					"+31 6 12345678"
				));
			}

			   if (registrations.findByEmail("employee@inholland.nl").isEmpty()) {
				   registrations.save(new UserRegistration(
					   "Employee",
					   "User",
					   "employee@inholland.nl",
					   passwordEncoder.encode("Password123!"),
					   "EMPLOYEE",
					   true,
					   null,
					   null,
					   "REGULAR"
				   ));
			   }

			   if (registrations.findByEmail("servicedesk@inholland.nl").isEmpty()) {
				   registrations.save(new UserRegistration(
					   "ServiceDesk",
					   "User",
					   "servicedesk@inholland.nl",
					   passwordEncoder.encode("Password123!"),
					   "EMPLOYEE",
					   true,
					   null,
					   null,
					   "SERVICE_DESK"
				   ));
			   }
		};
	}
}

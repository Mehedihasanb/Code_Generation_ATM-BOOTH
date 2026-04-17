package com.example.backend.config;

import com.example.backend.domain.Message;
import com.example.backend.repository.MessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}

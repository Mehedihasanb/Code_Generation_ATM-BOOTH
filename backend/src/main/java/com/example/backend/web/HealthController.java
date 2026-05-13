package com.example.backend.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight endpoint so load balancers or monitoring can verify the process is up without touching the database.
 * Public in {@link com.example.backend.config.SecurityConfig}; safe to call frequently.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health")
public class HealthController {

	@GetMapping("/health")
	@Operation(summary = "Liveness check for the API (no auth)")
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}
}

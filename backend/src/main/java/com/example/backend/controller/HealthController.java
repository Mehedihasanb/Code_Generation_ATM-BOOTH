package com.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Health")
public class HealthController {

	// quick check if backend is alive, no token needed
	// render and github pages use this to know the api is running
	@GetMapping("/health")
	@Operation(summary = "Liveness check for the API (no auth)")
	public Map<String, String> health() {
		// nothing else to call, just return UP from here
		return Map.of("status", "UP");
	}
}

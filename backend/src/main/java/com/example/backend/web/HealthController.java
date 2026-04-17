package com.example.backend.web;

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

	@GetMapping("/health")
	@Operation(summary = "Liveness check for the API")
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}
}

package com.example.backend.controller;

import com.example.backend.dto.UpdateCustomerLimitsRequest;
import com.example.backend.dto.UpdateCustomerLimitsResponse;
import com.example.backend.service.CustomerLimitsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Customer limits")
public class UserLimitsController {

	private final CustomerLimitsService customerLimitsService;

	public UserLimitsController(CustomerLimitsService customerLimitsService) {
		this.customerLimitsService = customerLimitsService;
	}

	@PutMapping("/{id}/limits")
	@PreAuthorize("hasRole('EMPLOYEE')")
	@Operation(summary = "Update a customer's absolute and/or daily transfer limits (employee only)")
	@SecurityRequirement(name = "bearerAuth")
	public UpdateCustomerLimitsResponse updateCustomerLimits(
			@PathVariable Long id,
			@Valid @RequestBody UpdateCustomerLimitsRequest request) {
		return customerLimitsService.updateCustomerLimits(id, request);
	}
}

package com.example.backend.controller;

import com.example.backend.service.CustomerDirectoryService;
import com.example.backend.dto.CustomerDirectoryRow;
import com.example.backend.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Customer directory")
public class CustomerDirectoryController {

	private final CustomerDirectoryService customerDirectoryService;

	public CustomerDirectoryController(CustomerDirectoryService customerDirectoryService) {
		this.customerDirectoryService = customerDirectoryService;
	}

	@GetMapping("/users")
	@Operation(summary = "Paginated customers; optionally search by exact name or IBAN")
	@SecurityRequirement(name = "bearerAuth")
	public PageResponse<CustomerDirectoryRow> listCustomers(
			@Parameter(description = "When false, only customers with no bank accounts yet") @RequestParam(required = false) Boolean hasAccount,
			@Parameter(description = "Exact first name for customer search") @RequestParam(required = false) String firstName,
			@Parameter(description = "Exact last name for customer search") @RequestParam(required = false) String lastName,
			@Parameter(description = "Exact IBAN for customer search") @RequestParam(required = false) String iban,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return customerDirectoryService.listCustomerDirectory(pageable, hasAccount, firstName, lastName, iban);
	}
}

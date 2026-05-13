package com.example.backend.web;

import com.example.backend.service.CustomerDirectoryService;
import com.example.backend.web.dto.CustomerDirectoryRow;
import com.example.backend.web.dto.PageResponse;
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
	@Operation(summary = "Paginated customers; use hasAccount=false for customers without any bank account (employee only)")
	@SecurityRequirement(name = "bearerAuth")
	public PageResponse<CustomerDirectoryRow> listCustomers(
		@Parameter(description = "When false, only customers with no bank accounts yet")
		@RequestParam(required = false) Boolean hasAccount,
		@ParameterObject @PageableDefault(size = 20) Pageable pageable
	) {
		return customerDirectoryService.listCustomerDirectory(pageable, hasAccount);
	}
}

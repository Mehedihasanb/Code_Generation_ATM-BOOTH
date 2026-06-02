package com.example.backend.controller;

import com.example.backend.dto.AtmDepositRequest;
import com.example.backend.dto.AtmDepositResponse;
import com.example.backend.dto.AtmWithdrawRequest;
import com.example.backend.dto.AtmWithdrawResponse;
import com.example.backend.service.AtmDepositService;
import com.example.backend.service.AtmWithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/atm")
@Tag(name = "ATM")
public class AtmController {

	private final AtmWithdrawService atmWithdrawService;
	private final AtmDepositService atmDepositService;

	public AtmController(AtmWithdrawService atmWithdrawService, AtmDepositService atmDepositService) {
		this.atmWithdrawService = atmWithdrawService;
		this.atmDepositService = atmDepositService;
	}

	@PostMapping("/withdraw")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Withdraw cash from the customer's checking account (ATM)")
	@SecurityRequirement(name = "bearerAuth")
	public AtmWithdrawResponse withdraw(
			@Valid @RequestBody AtmWithdrawRequest request,
			Authentication authentication) {
		return atmWithdrawService.withdraw(request, authentication.getName());
	}

	@PostMapping("/deposit")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Deposit cash to the customer's checking account (ATM)")
	@SecurityRequirement(name = "bearerAuth")
	public AtmDepositResponse deposit(
			@Valid @RequestBody AtmDepositRequest request,
			Authentication authentication) {
		return atmDepositService.deposit(request, authentication.getName());
	}
}

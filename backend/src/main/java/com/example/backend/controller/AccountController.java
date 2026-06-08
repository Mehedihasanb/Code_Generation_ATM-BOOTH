package com.example.backend.controller;

import com.example.backend.service.AccountService;
import com.example.backend.dto.AccountSummaryResponse;
import com.example.backend.dto.CheckingAccountOptionRow;
import com.example.backend.dto.CreateAccountsRequest;
import com.example.backend.dto.CreatedAccountsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import java.util.List;


@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	// employee only, this is the approve button basically
	// takes pending customer id + limits, service makes checking and savings with new ibans
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Approve customer: create checking + savings with limits (employee only)")
	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasRole('EMPLOYEE')")
	public CreatedAccountsResponse createAccounts(@Valid @RequestBody CreateAccountsRequest createAccountsRequest) {
		return accountService.createCheckingAndSavingsAccounts(createAccountsRequest);
	}

	// employee only, close an account by iban
	// sets it inactive so transfers cant use it anymore
	@PutMapping("/{iban}/close")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Close an account by IBAN (employee only); inactive accounts reject new transfers later")
	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasRole('EMPLOYEE')")
	public void closeAccount(@PathVariable String iban) {
		accountService.closeAccountByIban(iban);
	}

	// customer logged in with token sees their accounts
	// uses email from authentication to find the right customer
	@GetMapping("/mine")
	@Operation(summary = "Get current customer's accounts and combined balance")
	@SecurityRequirement(name = "bearerAuth")
	public AccountSummaryResponse getMyAccounts(Authentication authentication) {
		return accountService.getMyAccounts(authentication.getName());
	}

	// employee only, returns all checking accounts
	// used on force transfer page so employee can pick from and to accounts
	@GetMapping("/checking-options")
	@PreAuthorize("hasRole('EMPLOYEE')")
	@Operation(summary = "Get all checking accounts for employee dropdowns")
	@SecurityRequirement(name = "bearerAuth")
	public List<CheckingAccountOptionRow> getCheckingOptions() {
		return accountService.getAllCheckingAccountsForDropdown();
	}
}

package com.example.backend.web;

import com.example.backend.service.AccountService;
import com.example.backend.web.dto.CreateAccountsRequest;
import com.example.backend.web.dto.CreatedAccountsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Approve customer: create checking + savings with limits (employee only)")
	@SecurityRequirement(name = "bearerAuth")
	public CreatedAccountsResponse createAccounts(@Valid @RequestBody CreateAccountsRequest createAccountsRequest) {
		return accountService.createCheckingAndSavingsAccounts(createAccountsRequest);
	}

	@PutMapping("/{iban}/close")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Close an account by IBAN (employee only); inactive accounts reject new transfers later")
	@SecurityRequirement(name = "bearerAuth")
	public void closeAccount(@PathVariable String iban) {
		accountService.closeAccountByIban(iban);
	}
}

package com.example.backend.controllers;

import com.example.backend.dtos.AccountQuery;
import com.example.backend.dtos.AccountUpdateRequest;
import com.example.backend.dtos.EmployeeAccountResponse;
import com.example.backend.dtos.OwnAccountResponse;
import com.example.backend.dtos.TransferTargetResponse;
import com.example.backend.entities.Account;
import com.example.backend.entities.User;
import com.example.backend.mappers.AccountMapper;
import com.example.backend.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("accounts")
public class AccountRestController {

    private final AccountService accounts;
    private final AccountMapper toResponse;

    public AccountRestController(AccountService accounts, AccountMapper toResponse) {
        this.accounts = accounts;
        this.toResponse = toResponse;
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Page<EmployeeAccountResponse> listForEmployee(
            @ModelAttribute AccountQuery filters,
            @PageableDefault(size = 20) Pageable page) {
        return employeePage(accounts.getAll(filters, page));    
    }

    // Employee updates limits on an account (minimum balance + daily transfer cap)
    @PatchMapping("/{iban}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public EmployeeAccountResponse applyUpdate(
            @PathVariable String iban,
            @Valid @RequestBody AccountUpdateRequest body) {
        Account updated = accounts.updateAccount(iban, body);
        return toResponse.toEmployeeResponse(updated);
    }

    
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication)")
    public Page<OwnAccountResponse> myAccounts(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable page) {
        return ownPage(accounts.getOwnAccounts(user.getId(), page));
    }

    @GetMapping("/transfer-targets")
    @PreAuthorize("hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication)")
    public Page<TransferTargetResponse> findRecipientsByName(
            @AuthenticationPrincipal User user,
            @RequestParam String name,
            @PageableDefault(size = 20) Pageable page) {
        return targetPage(accounts.searchTransferTargets(user.getId(), name, page));
    }

    private Page<EmployeeAccountResponse> employeePage(Page<Account> page) {
        return page.map(toResponse::toEmployeeResponse);
    }

    private Page<OwnAccountResponse> ownPage(Page<Account> page) {
        return page.map(toResponse::toOwnResponse);
    }

    private Page<TransferTargetResponse> targetPage(Page<Account> page) {
        return page.map(toResponse::toTransferTargetResponse);
    }
}

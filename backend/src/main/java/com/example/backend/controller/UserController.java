package com.example.backend.controller;

import com.example.backend.dto.CustomerDirectoryRow;
import com.example.backend.dto.SystemTransactionRow;
import com.example.backend.service.AccountService;
import com.example.backend.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public UserController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/search")
    @Operation(summary = "Find customer IBANs by exact first and last name")
    @SecurityRequirement(name = "bearerAuth")
    public List<CustomerDirectoryRow> searchUsers(
            @RequestParam String firstName,
            @RequestParam String lastName) {
        return accountService.searchCustomersByName(firstName, lastName);
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Get full transaction history for a specific customer ID")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<SystemTransactionRow>> getUserTransactions(
            @PathVariable Long id,
            Pageable pageable) {

        Page<SystemTransactionRow> transactions = transactionService.getTransactionsByUserId(id, pageable);
        return ResponseEntity.ok(transactions);
    }
}
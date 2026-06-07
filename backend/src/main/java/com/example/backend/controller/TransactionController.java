package com.example.backend.controller;

import com.example.backend.dto.SystemTransactionRow;
import com.example.backend.dto.TransactionHistoryRow;
import com.example.backend.dto.TransferRequest;
import com.example.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDate;
import java.math.BigDecimal;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Initiate a transfer (Customer or Employee)")
    @SecurityRequirement(name = "bearerAuth")
    public void createTransaction(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        transactionService.processTransfer(request, authentication);
    }

    @GetMapping
    @Operation(summary = "Get transactions (Filtered for Customers, All for Employees)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getTransactions(
            @RequestParam(required = false) String accountIban,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false, defaultValue = "eq") String amountOperator,
            @RequestParam(required = false) String counterpartIban,
            Pageable pageable,
            Authentication authentication) {

        boolean isEmployee = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_EMPLOYEE"));

        if (isEmployee && accountIban == null) {
            Page<SystemTransactionRow> allTransactions = transactionService.getAllSystemTransactions(
                    startDate, endDate, amount, amountOperator, pageable);
            return ResponseEntity.ok(allTransactions);
        }

        if (accountIban == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountIban is required for customer queries.");
        }

        Page<TransactionHistoryRow> customerTransactions = transactionService.getTransactionHistory(
                accountIban, startDate, endDate, amount, amountOperator, counterpartIban, authentication.getName(),
                pageable);

        return ResponseEntity.ok(customerTransactions);
    }

}
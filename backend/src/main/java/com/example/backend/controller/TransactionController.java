package com.example.backend.controller;

import com.example.backend.dto.TransferRequest;
import com.example.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.backend.dto.TransactionHistoryRow;

import java.security.Principal;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Transfer money between accounts (Customer only)")
    @SecurityRequirement(name = "bearerAuth")
    public void executeTransfer(@Valid @RequestBody TransferRequest request, Principal principal) {
        transactionService.processTransfer(request, principal.getName());
    }

    @GetMapping
    @Operation(summary = "Get paginated transaction history for a specific account")
    @SecurityRequirement(name = "bearerAuth")
    public Page<TransactionHistoryRow> getTransactions(
            @RequestParam String accountIban,
            Pageable pageable,
            Principal principal) {

        return transactionService.getTransactionHistory(accountIban, principal.getName(), pageable);
    }
}
package com.example.backend.controllers;

import com.example.backend.dtos.TransactionCreateRequest;
import com.example.backend.dtos.TransactionFilterParams;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.entities.Transaction;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.UserRole;
import com.example.backend.mappers.TransactionMapper;
import com.example.backend.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("transactions")
public class TransactionRestController {

    private final TransactionService transactions;
    private final TransactionMapper toResponse;

    public TransactionRestController(TransactionService transactions, TransactionMapper toResponse) {
        this.transactions = transactions;
        this.toResponse = toResponse;
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE') or (hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication))")
    public Page<TransactionResponse> listHistory(
            @AuthenticationPrincipal User user,
            @ModelAttribute TransactionFilterParams filters,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable page) {
        scopeFiltersFor(user, filters);
        return historyPage(transactions.getAll(filters, page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') or (hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication))")
    public TransactionResponse findById(
            @AuthenticationPrincipal User user,
            @PathVariable int id) {
        Transaction transaction = transactions.getById(id);
        ensureCanView(user, transaction);
        return toDto(transaction);
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE') or (hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication))")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse submit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransactionCreateRequest body) {
        Transaction created = transactions.create(body, user);
        return toDto(created);
    }

    private void scopeFiltersFor(User user, TransactionFilterParams filters) {
        if (user.getRole() != UserRole.EMPLOYEE) {
            filters.setCustomerId(user.getId());
        }
    }

    private void ensureCanView(User user, Transaction transaction) {
        if (user.getRole() != UserRole.EMPLOYEE) {
            transactions.assertCustomerCanView(transaction, user);
        }
    }

    private Page<TransactionResponse> historyPage(Page<Transaction> page) {
        return page.map(toResponse::toResponse);
    }

    private TransactionResponse toDto(Transaction transaction) {
        return toResponse.toResponse(transaction);
    }
}

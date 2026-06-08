package com.example.backend.controller;

import com.example.backend.dto.CustomerDirectoryRow;
import com.example.backend.dto.SystemTransactionRow;
import com.example.backend.service.AccountService;
import com.example.backend.service.TransactionService;
import com.example.backend.service.UserDeletionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// employee user routes - directory search, tx history, delete/reactivate accounts
@RestController
@RequestMapping("/users")
public class UserController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final UserDeletionService userDeletionService;

    public UserController(
            AccountService accountService,
            TransactionService transactionService,
            UserDeletionService userDeletionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.userDeletionService = userDeletionService;
    }

    @GetMapping("/search")
    @Operation(summary = "Find customer IBANs by exact first and last name")
    @SecurityRequirement(name = "bearerAuth")
    public List<CustomerDirectoryRow> searchUsers(
            @RequestParam String firstName, // these annotations r used to sort, filter, search across a collection of
                                            // resources
            @RequestParam String lastName) {
        return accountService.searchCustomersByName(firstName, lastName);
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Get full transaction history for a specific customer ID")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<SystemTransactionRow>> getUserTransactions(
            @PathVariable Long id, // this @ identifies a specific resource
            Pageable pageable) {

        Page<SystemTransactionRow> transactions = transactionService.getTransactionsByUserId(id, pageable);
        return ResponseEntity.ok(transactions);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Employee deletes a user account (soft by default, permanent when permanent=true)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('EMPLOYEE')") // who can call it - rules inside UserDeletionPolicy
    // DELETE /users/{id} from employee directory - soft or permanent same as /auth/me
    public void deleteUser(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean permanent,
            Authentication authentication) {
        userDeletionService.deleteAccount(authentication.getName(), id, permanent);
    }

    @PostMapping("/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Employee reactivates a soft-deleted user account")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('EMPLOYEE')")
    // undo soft delete - flips deleted=false and turns accounts active again
    public void reactivateUser(@PathVariable Long id, Authentication authentication) {
        userDeletionService.reactivateAccount(authentication.getName(), id);
    }
}
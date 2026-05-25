package com.example.backend.controller;

import com.example.backend.dto.CustomerDirectoryRow;
import com.example.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/search")
public class UserController {

    private final AccountService accountService;

    // injecting AccountService so it can reuse existing logic
    public UserController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @Operation(summary = "Find customer IBANs by exact first and last name")
    @SecurityRequirement(name = "bearerAuth")
    public List<CustomerDirectoryRow> searchUsers(
            @RequestParam String firstName,
            @RequestParam String lastName) {
        return accountService.searchCustomersByName(firstName, lastName);
    }
}
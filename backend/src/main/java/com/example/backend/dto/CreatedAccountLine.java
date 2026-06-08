package com.example.backend.dto;

// one opened account line inside CreatedAccountsResponse
public record CreatedAccountLine(String iban, String accountType) {
}

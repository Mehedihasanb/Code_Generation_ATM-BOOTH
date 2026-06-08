package com.example.backend.dto;

// dropdown option for employee force transfer (owner name + checking IBAN)
public record CheckingAccountOptionRow(
        String iban,
        String ownerName) {
}
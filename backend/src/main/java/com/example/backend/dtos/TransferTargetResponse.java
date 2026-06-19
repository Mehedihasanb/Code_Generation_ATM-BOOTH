package com.example.backend.dtos;

public record TransferTargetResponse(
        String iban,
        String firstName,
        String lastName
) {}

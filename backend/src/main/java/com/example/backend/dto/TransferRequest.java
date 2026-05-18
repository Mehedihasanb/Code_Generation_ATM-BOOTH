package com.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Sender IBAN is required") String fromIban,

        @NotBlank(message = "Receiver IBAN is required") String toIban,

        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero") BigDecimal amount,

        String description) {
}
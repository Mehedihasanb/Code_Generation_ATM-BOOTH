package com.example.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccountSummaryResponse(
        String customerName,
        BigDecimal combinedBalance,
        List<AccountDetail> accounts) {
}
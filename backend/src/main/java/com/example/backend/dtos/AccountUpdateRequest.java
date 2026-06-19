package com.example.backend.dtos;

import com.example.backend.entities.enums.AccountStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * PATCH body for {@code PATCH /accounts/{iban}} — all fields optional, but at least one is required.
 */
public class AccountUpdateRequest {

    private AccountStatus status;

    @PositiveOrZero(message = "absoluteTransferLimit must be zero or greater")
    private BigDecimal absoluteTransferLimit;

    @PositiveOrZero(message = "dailyTransferLimit must be zero or greater")
    private BigDecimal dailyTransferLimit;

    public AccountUpdateRequest() {
    }

    public AccountUpdateRequest(BigDecimal absoluteTransferLimit,
                              BigDecimal dailyTransferLimit,
                              AccountStatus status) {
        this.absoluteTransferLimit = absoluteTransferLimit;
        this.dailyTransferLimit = dailyTransferLimit;
        this.status = status;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public BigDecimal getAbsoluteTransferLimit() {
        return absoluteTransferLimit;
    }

    public void setAbsoluteTransferLimit(BigDecimal absoluteTransferLimit) {
        this.absoluteTransferLimit = absoluteTransferLimit;
    }

    public BigDecimal getDailyTransferLimit() {
        return dailyTransferLimit;
    }

    public void setDailyTransferLimit(BigDecimal dailyTransferLimit) {
        this.dailyTransferLimit = dailyTransferLimit;
    }

    @AssertTrue(message = "At least one field must be provided")
    public boolean hasAtLeastOneField() {
        return absoluteTransferLimit != null || dailyTransferLimit != null || status != null;
    }
}

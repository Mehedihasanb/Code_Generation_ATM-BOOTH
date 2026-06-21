package com.example.backend.dtos;

import com.example.backend.entities.enums.AccountStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;


public class AccountUpdateRequest {

    private AccountStatus status;

    @PositiveOrZero(message = "minimumBalanceLimit must be zero or greater")
    private BigDecimal minimumBalanceLimit;

    @PositiveOrZero(message = "dailyTransferLimit must be zero or greater")
    private BigDecimal dailyTransferLimit;

    public AccountUpdateRequest() {
    }

    public AccountUpdateRequest(BigDecimal minimumBalanceLimit,
                              BigDecimal dailyTransferLimit,
                              AccountStatus status) {
        this.minimumBalanceLimit = minimumBalanceLimit;
        this.dailyTransferLimit = dailyTransferLimit;
        this.status = status;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public BigDecimal getMinimumBalanceLimit() {
        return minimumBalanceLimit;
    }

    public void setMinimumBalanceLimit(BigDecimal minimumBalanceLimit) {
        this.minimumBalanceLimit = minimumBalanceLimit;
    }

    public BigDecimal getDailyTransferLimit() {
        return dailyTransferLimit;
    }

    public void setDailyTransferLimit(BigDecimal dailyTransferLimit) {
        this.dailyTransferLimit = dailyTransferLimit;
    }

    @AssertTrue(message = "At least one field must be provided")
    public boolean hasAtLeastOneField() {
        return minimumBalanceLimit != null || dailyTransferLimit != null || status != null;
    }
}

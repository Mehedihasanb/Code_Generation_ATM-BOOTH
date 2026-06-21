package com.example.backend.domain.rules;

import com.example.backend.entities.Account;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountRules {

    public void validateClosable(Account account) {
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Account must have a zero balance before it can be closed");
        }
    }

    public void requireLimitsForNewAccount(BigDecimal minimumBalanceLimit, BigDecimal dailyLimit) {
        if (minimumBalanceLimit == null) {
            throw new IllegalArgumentException("A minimum balance limit is required when opening accounts");
        }
        if (dailyLimit == null) {
            throw new IllegalArgumentException("A daily transfer limit is required when opening accounts");
        }
        validateLimits(minimumBalanceLimit, dailyLimit);
    }

    public void validateLimits(BigDecimal minimumBalanceLimit, BigDecimal dailyLimit) {
        rejectIfNegative(minimumBalanceLimit, "minimumBalanceLimit");
        rejectIfNegative(dailyLimit, "dailyTransferLimit");
    }

    private void rejectIfNegative(BigDecimal value, String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }
}

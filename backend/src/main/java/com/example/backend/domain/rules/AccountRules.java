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

    public void requireLimitsForNewAccount(BigDecimal absoluteLimit, BigDecimal dailyLimit) {
        if (absoluteLimit == null) {
            throw new IllegalArgumentException("An absolute transfer limit is required when opening accounts");
        }
        if (dailyLimit == null) {
            throw new IllegalArgumentException("A daily transfer limit is required when opening accounts");
        }
        validateLimits(absoluteLimit, dailyLimit);
    }

    public void validateLimits(BigDecimal absoluteLimit, BigDecimal dailyLimit) {
        rejectIfNegative(absoluteLimit, "absoluteTransferLimit");
        rejectIfNegative(dailyLimit, "dailyTransferLimit");
    }

    private void rejectIfNegative(BigDecimal value, String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }
}

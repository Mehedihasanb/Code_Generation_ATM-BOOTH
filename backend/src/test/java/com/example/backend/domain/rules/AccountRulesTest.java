package com.example.backend.domain.rules;

import com.example.backend.entities.Account;
import com.example.backend.entities.enums.AccountStatus;
import com.example.backend.entities.enums.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Account business rules")
class AccountRulesTest {

    private AccountRules rules;
    private Account account;

    @BeforeEach
    void setUp() {
        rules = new AccountRules();

        account = new Account();
        account.setIban("NL95INHO0000000001");
        account.setType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("100.00"));
        account.setAbsoluteTransferLimit(BigDecimal.ZERO);
        account.setDailyTransferLimit(new BigDecimal("500.00"));
    }

    @Test
    void validateClosable_zeroBalance_doesNotThrow() {
        account.setBalance(BigDecimal.ZERO);
        assertDoesNotThrow(() -> rules.validateClosable(account));
    }

    @Test
    void validateClosable_positiveBalance_throwsIllegalArgument() {
        account.setBalance(new BigDecimal("100.00"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rules.validateClosable(account));
        assertTrue(ex.getMessage().contains("zero balance"));
    }

    @Test
    void validateClosable_negativeBalance_throwsIllegalArgument() {
        account.setBalance(new BigDecimal("-50.00"));
        assertThrows(IllegalArgumentException.class, () -> rules.validateClosable(account));
    }

    @Test
    void requireLimitsForNewAccount_bothProvided_doesNotThrow() {
        assertDoesNotThrow(() -> rules.requireLimitsForNewAccount(
                new BigDecimal("500.00"), new BigDecimal("2000.00")));
    }

    @Test
    void requireLimitsForNewAccount_missingAbsoluteLimit_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rules.requireLimitsForNewAccount(null, BigDecimal.ZERO));
        assertTrue(ex.getMessage().contains("absolute transfer limit is required"));
    }

    @Test
    void requireLimitsForNewAccount_missingDailyLimit_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rules.requireLimitsForNewAccount(BigDecimal.ZERO, null));
        assertTrue(ex.getMessage().contains("daily transfer limit is required"));
    }

    @Test
    void requireLimitsForNewAccount_negativeLimit_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rules.requireLimitsForNewAccount(new BigDecimal("-1.00"), BigDecimal.ZERO));
        assertTrue(ex.getMessage().contains("cannot be negative"));
    }

    @Test
    void validateLimits_bothNull_doesNotThrow() {
        assertDoesNotThrow(() -> rules.validateLimits(null, null));
    }

    @Test
    void validateLimits_bothZero_doesNotThrow() {
        assertDoesNotThrow(() -> rules.validateLimits(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void validateLimits_positiveValues_doesNotThrow() {
        assertDoesNotThrow(() -> rules.validateLimits(
                new BigDecimal("500.00"), new BigDecimal("2000.00")));
    }

    @Test
    void validateLimits_negativeAbsoluteLimit_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rules.validateLimits(new BigDecimal("-1.00"), null));
        assertTrue(ex.getMessage().contains("cannot be negative"));
    }

    @Test
    void validateLimits_negativeDailyLimit_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rules.validateLimits(null, new BigDecimal("-0.01")));
        assertTrue(ex.getMessage().contains("dailyTransferLimit cannot be negative"));
    }

    @Test
    void validateLimits_bothNegative_throwsOnFirstViolation() {
        assertThrows(IllegalArgumentException.class,
                () -> rules.validateLimits(new BigDecimal("-10"), new BigDecimal("-10")));
    }
}

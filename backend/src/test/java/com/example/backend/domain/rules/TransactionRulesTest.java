package com.example.backend.domain.rules;

import com.example.backend.dtos.TransactionCreateRequest;
import com.example.backend.entities.Account;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.AccountStatus;
import com.example.backend.entities.enums.AccountType;
import com.example.backend.entities.enums.TransactionType;
import com.example.backend.entities.enums.UserRole;
import com.example.backend.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// TransactionRules is now a pure domain object with no repository dependencies — no Mockito needed.
@DisplayName("Transaction business rules")
class TransactionRulesTest {

    private TransactionRules rules;

    private Account activeFromAccount;
    private Account activeToAccount;
    private Account otherCustomerCheckingAccount;
    private Account otherCustomerSavingsAccount;
    private Account closedAccount;
    private User customerUser;
    private User employeeUser;
    private User otherCustomer;

    private static final String FROM_IBAN = "NL01BANK0000000001";
    private static final String TO_IBAN   = "NL02BANK0000000002";

    /** Zero outgoing spend today — the common happy-path starting point. */
    private static final BigDecimal NO_PRIOR_SPEND = BigDecimal.ZERO;

    @BeforeEach
    void setUp() {
        rules = new TransactionRules();

        customerUser = new User();
        customerUser.setId(1);
        customerUser.setRole(UserRole.CUSTOMER);

        employeeUser = new User();
        employeeUser.setId(2);
        employeeUser.setRole(UserRole.EMPLOYEE);

        otherCustomer = new User();
        otherCustomer.setId(3);
        otherCustomer.setRole(UserRole.CUSTOMER);

        activeFromAccount = new Account();
        activeFromAccount.setIban(FROM_IBAN);
        activeFromAccount.setType(AccountType.CHECKING);
        activeFromAccount.setStatus(AccountStatus.ACTIVE);
        activeFromAccount.setBalance(new BigDecimal("1000.00"));
        activeFromAccount.setAbsoluteTransferLimit(new BigDecimal("-500.00"));
        activeFromAccount.setDailyTransferLimit(new BigDecimal("2000.00"));
        activeFromAccount.setUser(customerUser);

        activeToAccount = new Account();
        activeToAccount.setIban(TO_IBAN);
        activeToAccount.setType(AccountType.SAVINGS);
        activeToAccount.setStatus(AccountStatus.ACTIVE);
        activeToAccount.setUser(customerUser);

        otherCustomerCheckingAccount = new Account();
        otherCustomerCheckingAccount.setIban("NL04BANK0000000004");
        otherCustomerCheckingAccount.setType(AccountType.CHECKING);
        otherCustomerCheckingAccount.setStatus(AccountStatus.ACTIVE);
        otherCustomerCheckingAccount.setUser(otherCustomer);

        otherCustomerSavingsAccount = new Account();
        otherCustomerSavingsAccount.setIban("NL05BANK0000000005");
        otherCustomerSavingsAccount.setType(AccountType.SAVINGS);
        otherCustomerSavingsAccount.setStatus(AccountStatus.ACTIVE);
        otherCustomerSavingsAccount.setUser(otherCustomer);

        closedAccount = new Account();
        closedAccount.setIban("NL03BANK0000000003");
        closedAccount.setType(AccountType.CHECKING);
        closedAccount.setStatus(AccountStatus.CLOSED);
        closedAccount.setUser(customerUser);
    }

    // --- validateTransfer (high-level) ---

    @Test
    void validateTransfer_happyPath_doesNotThrow() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertDoesNotThrow(
                () -> rules.validateTransfer(
                        request, activeFromAccount, activeToAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateTransfer_throwsWhenFromIbanIsNull() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThrows(BadRequestException.class,
                () -> rules.validateTransfer(
                        request, activeFromAccount, activeToAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateTransfer_throwsWhenSameAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, FROM_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThrows(BadRequestException.class,
                () -> rules.validateTransfer(
                        request, activeFromAccount, activeFromAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateTransfer_throwsWhenSourceAccountIsClosed() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThrows(BadRequestException.class,
                () -> rules.validateTransfer(
                        request, closedAccount, activeToAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateTransfer_throwsWhenCustomerUsesAnotherCustomersAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThrows(ResponseStatusException.class,
                () -> rules.validateTransfer(
                        request, activeFromAccount, activeToAccount, otherCustomer, NO_PRIOR_SPEND));
    }

    @Test
    void validateTransfer_allowsCustomerExternalTransferToCheckingAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, otherCustomerCheckingAccount.getIban(), null,
                new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertDoesNotThrow(
                () -> rules.validateTransfer(
                        request, activeFromAccount, otherCustomerCheckingAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateTransfer_throwsWhenCustomerExternalTransferTargetsSavingsAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, otherCustomerSavingsAccount.getIban(), null,
                new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThrows(BadRequestException.class,
                () -> rules.validateTransfer(
                        request, activeFromAccount, otherCustomerSavingsAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateTransfer_allowsCustomerTransferBetweenOwnAccounts() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertDoesNotThrow(
                () -> rules.validateTransfer(
                        request, activeFromAccount, activeToAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateTransfer_throwsWhenAbsoluteLimitBreached() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("1600.00"), TransactionType.TRANSFER, null);

        // balance 1000 - 1600 = -600, below absoluteTransferLimit of -500
        assertThrows(BadRequestException.class,
                () -> rules.validateTransfer(
                        request, activeFromAccount, activeToAccount, customerUser, NO_PRIOR_SPEND));
    }

    // --- validateDeposit (high-level) ---

    @Test
    void validateDeposit_happyPath_doesNotThrow() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.DEPOSIT, null);

        assertDoesNotThrow(() -> rules.validateDeposit(request, activeToAccount));
    }

    @Test
    void validateDeposit_throwsWhenToIbanIsNull() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, null, null, new BigDecimal("100.00"), TransactionType.DEPOSIT, null);

        assertThrows(BadRequestException.class,
                () -> rules.validateDeposit(request, activeToAccount));
    }

    @Test
    void validateDeposit_throwsWhenAccountIsClosed() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.DEPOSIT, null);

        assertThrows(BadRequestException.class,
                () -> rules.validateDeposit(request, closedAccount));
    }

    // --- validateWithdrawal (high-level) ---

    @Test
    void validateWithdrawal_happyPath_doesNotThrow() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, null, null, new BigDecimal("100.00"), TransactionType.WITHDRAWAL, null);

        assertDoesNotThrow(
                () -> rules.validateWithdrawal(
                        request, activeFromAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateWithdrawal_throwsWhenFromIbanIsNull() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, null, null, new BigDecimal("100.00"), TransactionType.WITHDRAWAL, null);

        assertThrows(BadRequestException.class,
                () -> rules.validateWithdrawal(
                        request, activeFromAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateWithdrawal_throwsWhenAccountIsClosed() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, null, null, new BigDecimal("100.00"), TransactionType.WITHDRAWAL, null);

        assertThrows(BadRequestException.class,
                () -> rules.validateWithdrawal(
                        request, closedAccount, customerUser, NO_PRIOR_SPEND));
    }

    @Test
    void validateWithdrawal_throwsWhenCustomerUsesAnotherCustomersAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, null, null, new BigDecimal("100.00"), TransactionType.WITHDRAWAL, null);

        assertThrows(ResponseStatusException.class,
                () -> rules.validateWithdrawal(
                        request, activeFromAccount, otherCustomer, NO_PRIOR_SPEND));
    }

    // --- requireSourceOwner (individual rule — employee bypass is worth testing directly) ---

    @Test
    void requireSourceOwner_throwsWhenCustomerUsesAnotherCustomersAccount() {
        assertThrows(ResponseStatusException.class,
                () -> rules.requireSourceOwner(activeFromAccount, otherCustomer));
    }

    @Test
    void requireSourceOwner_allowsCustomerUsingOwnAccount() {
        assertDoesNotThrow(() -> rules.requireSourceOwner(activeFromAccount, customerUser));
    }

    @Test
    void requireSourceOwner_allowsEmployeeToUseAnyAccount() {
        assertDoesNotThrow(() -> rules.requireSourceOwner(activeFromAccount, employeeUser));
    }

    // --- validateAbsoluteLimit (individual rule) ---

    @Test
    void validateAbsoluteLimit_throwsWhenAmountBreachesLimit() {
        // balance 1000 - 1600 = -600, below absoluteTransferLimit of -500
        assertThrows(BadRequestException.class,
                () -> rules.validateAbsoluteLimit(activeFromAccount, new BigDecimal("1600.00")));
    }

    @Test
    void validateAbsoluteLimit_allowsAmountWithinLimit() {
        // balance 1000 - 1400 = -400, above absoluteTransferLimit of -500
        assertDoesNotThrow(
                () -> rules.validateAbsoluteLimit(activeFromAccount, new BigDecimal("1400.00")));
    }

    // --- validateDailyLimit (individual rule — outgoingToday is now supplied directly, no repo needed) ---

    @Test
    void validateDailyLimit_throwsWhenDailyLimitExceeded() {
        // 1500 already transferred today + 600 new = 2100 > dailyTransferLimit of 2000
        assertThrows(BadRequestException.class,
                () -> rules.validateDailyLimit(
                        activeFromAccount, new BigDecimal("1500.00"), new BigDecimal("600.00")));
    }

    @Test
    void validateDailyLimit_countsPriorWithdrawalsAgainstLimit() {
        // 1500 already withdrawn today + 600 new = 2100 > dailyTransferLimit of 2000
        assertThrows(BadRequestException.class,
                () -> rules.validateDailyLimit(
                        activeFromAccount, new BigDecimal("1500.00"), new BigDecimal("600.00")));
    }

    @Test
    void validateDailyLimit_allowsWhenWithinDailyLimit() {
        // 0 transferred today + 500 = 500 < dailyTransferLimit of 2000
        assertDoesNotThrow(
                () -> rules.validateDailyLimit(
                        activeFromAccount, BigDecimal.ZERO, new BigDecimal("500.00")));
    }
}

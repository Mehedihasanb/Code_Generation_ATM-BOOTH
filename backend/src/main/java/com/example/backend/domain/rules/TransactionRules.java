package com.example.backend.domain.rules;

import com.example.backend.dtos.TransactionCreateRequest;
import com.example.backend.entities.Account;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.AccountStatus;
import com.example.backend.entities.enums.AccountType;
import com.example.backend.entities.enums.TransactionType;
import com.example.backend.entities.enums.UserRole;
import com.example.backend.exceptions.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Component
public class TransactionRules {

    public void validateTransfer(TransactionCreateRequest request,
                                 Account fromAccount,
                                 Account toAccount,
                                 User initiatedBy,
                                 BigDecimal outgoingToday) {
        requireTransferIbans(request.fromIban(), request.toIban());
        requireDifferentAccounts(request.fromIban(), request.toIban());
        requireActive(fromAccount, "Source");
        requireActive(toAccount, "Destination");
        requireSourceOwner(fromAccount, initiatedBy);
        requireExternalCheckingTarget(fromAccount, toAccount, initiatedBy);
        validateOutgoingLimits(fromAccount, outgoingToday, request.amount());
    }

    public void validateDeposit(TransactionCreateRequest request, Account toAccount) {
        requireIban(request.toIban(), "Deposit transactions require a destination IBAN");
        requireActive(toAccount, "Destination");
    }

    public void validateWithdrawal(TransactionCreateRequest request,
                                   Account fromAccount,
                                   User initiatedBy,
                                   BigDecimal outgoingToday) {
        requireIban(request.fromIban(), "Withdrawal transactions require a source IBAN");
        requireActive(fromAccount, "Source");
        requireSourceOwner(fromAccount, initiatedBy);
        validateOutgoingLimits(fromAccount, outgoingToday, request.amount());
    }

    public void rejectUnsupportedType(TransactionType type) {
        throw new BadRequestException("Transaction type not supported: " + type);
    }

    public void requireTransferIbans(String fromIban, String toIban) {
        if (isBlank(fromIban) || isBlank(toIban)) {
            throw new BadRequestException("A transfer needs both a source and destination IBAN");
        }
    }

    public void requireDifferentAccounts(String fromIban, String toIban) {
        if (fromIban.equals(toIban)) {
            throw new BadRequestException("Source and destination IBAN must be different");
        }
    }

    public void requireActive(Account account, String label) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("The " + label.toLowerCase() + " account is not active: " + account.getIban());
        }
    }

    public void requireSourceOwner(Account account, User initiatedBy) {
        if (isEmployee(initiatedBy)) {
            return;
        }
        if (account.getUser().getId() != initiatedBy.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to debit this account");
        }
    }

    public void requireExternalCheckingTarget(Account fromAccount, Account toAccount, User initiatedBy) {
        if (isEmployee(initiatedBy)) {
            return;
        }
        if (fromAccount.getUser().getId() == toAccount.getUser().getId()) {
            return;
        }
        if (toAccount.getType() != AccountType.CHECKING) {
            throw new BadRequestException("Customers may only transfer to another person's checking account");
        }
    }

    public void validateMinimumBalanceLimit(Account from, BigDecimal amount) {
        BigDecimal balanceAfter = from.getBalance().subtract(amount);
        if (balanceAfter.compareTo(from.getMinimumBalanceLimit()) < 0) {
            throw new BadRequestException(
                    "This transfer would leave the balance below the minimum allowed for IBAN " + from.getIban());
        }
    }

    public void validateDailyLimit(Account from, BigDecimal outgoingToday, BigDecimal amount) {
        if (outgoingToday.add(amount).compareTo(from.getDailyTransferLimit()) > 0) {
            throw new BadRequestException(
                    "This transfer would exceed the daily limit for IBAN " + from.getIban());
        }
    }

    private void validateOutgoingLimits(Account from, BigDecimal outgoingToday, BigDecimal amount) {
        validateMinimumBalanceLimit(from, amount);
        validateDailyLimit(from, outgoingToday, amount);
    }

    private void requireIban(String iban, String message) {
        if (isBlank(iban)) {
            throw new BadRequestException(message);
        }
    }

    private boolean isEmployee(User user) {
        return user.getRole() == UserRole.EMPLOYEE;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

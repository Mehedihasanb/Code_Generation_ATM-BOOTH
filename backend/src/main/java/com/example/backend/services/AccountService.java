package com.example.backend.services;

import com.example.backend.domain.rules.AccountRules;
import com.example.backend.dtos.AccountQuery;
import com.example.backend.dtos.AccountUpdateRequest;
import com.example.backend.entities.Account;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.AccountStatus;
import com.example.backend.entities.enums.AccountType;
import com.example.backend.exceptions.ResourceNotFoundException;
import com.example.backend.repositories.AccountRepository;
import com.example.backend.util.IbanGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountRules rules;
    private final IbanGenerator ibanGenerator;

    @Autowired
    public AccountService(AccountRepository accountRepository, AccountRules rules,
                          IbanGenerator ibanGenerator) {
        this.accountRepository = accountRepository;
        this.rules = rules;
        this.ibanGenerator = ibanGenerator;
    }

    @Transactional
    public List<Account> createAccountsForUser(User user, BigDecimal absoluteTransferLimit,
                                               BigDecimal dailyTransferLimit) {
        rules.requireLimitsForNewAccount(absoluteTransferLimit, dailyTransferLimit);

        Account checking = buildAccount(user, AccountType.CHECKING, absoluteTransferLimit, dailyTransferLimit);
        Account savings  = buildAccount(user, AccountType.SAVINGS,  absoluteTransferLimit, dailyTransferLimit);
        return accountRepository.saveAll(List.of(checking, savings));
    }

    public Page<Account> getAll(AccountQuery query, Pageable pageable) {
        return accountRepository.findAllFiltered(query, pageable);
    }

    public Page<Account> getOwnAccounts(int userId, Pageable pageable) {
        return accountRepository.findByUser_Id(userId, pageable);
    }

    public Page<Account> searchTransferTargets(int excludeUserId, String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return Page.empty(pageable);
        }
        return accountRepository.findTransferTargetsByCustomerName(excludeUserId, name.trim(), pageable);
    }

    public Account getByIban(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("No account for IBAN " + iban));
    }

    @Transactional
    public Account updateAccount(String iban, AccountUpdateRequest request) {
        rules.validateLimits(request.getAbsoluteTransferLimit(), request.getDailyTransferLimit());
        Account account = getByIban(iban);
        if (request.getStatus() == AccountStatus.CLOSED) {
            rules.validateClosable(account);
        }
        if (request.getAbsoluteTransferLimit() != null) {
            account.setAbsoluteTransferLimit(request.getAbsoluteTransferLimit());
        }
        if (request.getDailyTransferLimit() != null) {
            account.setDailyTransferLimit(request.getDailyTransferLimit());
        }
        if (request.getStatus() != null) {
            account.setStatus(request.getStatus());
        }
        return accountRepository.save(account);
    }

    private Account buildAccount(User user, AccountType type,
                                 BigDecimal absoluteTransferLimit, BigDecimal dailyTransferLimit) {
        return new Account(0, user, ibanGenerator.generate(), type,
                BigDecimal.ZERO, absoluteTransferLimit, dailyTransferLimit,
                AccountStatus.ACTIVE, LocalDateTime.now());
    }
}

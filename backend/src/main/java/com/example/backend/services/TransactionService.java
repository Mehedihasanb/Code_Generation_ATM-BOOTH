package com.example.backend.services;

import com.example.backend.domain.rules.TransactionRules;
import com.example.backend.dtos.TransactionCreateRequest;
import com.example.backend.dtos.TransactionFilterParams;
import com.example.backend.entities.Account;
import com.example.backend.entities.Transaction;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.TransactionType;
import com.example.backend.exceptions.BadRequestException;
import com.example.backend.exceptions.ResourceNotFoundException;
import com.example.backend.mappers.TransactionMapper;
import com.example.backend.repositories.AccountRepository;
import com.example.backend.repositories.TransactionRepository;
import com.example.backend.repositories.TransactionSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private static final List<TransactionType> OUTGOING_LIMIT_TYPES =
            List.of(TransactionType.TRANSFER, TransactionType.WITHDRAWAL);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionRules rules;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              TransactionMapper transactionMapper,
                              TransactionRules rules) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.transactionMapper = transactionMapper;
        this.rules = rules;
    }

    public Page<Transaction> getAll(TransactionFilterParams filters, Pageable pageable) {
        if (filters.getStartDate() != null && filters.getEndDate() != null
                && filters.getStartDate().isAfter(filters.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        return transactionRepository.findAll(TransactionSpecifications.fromFilters(filters), pageable);
    }

    public Transaction getById(int id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No transaction with id " + id));
    }


    public void assertCustomerCanView(Transaction transaction, User customer) {
        if (transaction.getInitiatedBy().getId() == customer.getId()) {
            return;
        }
        boolean ownsFromIban = transaction.getFromIban() != null
                && accountRepository.findByIban(transaction.getFromIban())
                        .map(a -> a.getUser().getId() == customer.getId())
                        .orElse(false);
        boolean ownsToIban = transaction.getToIban() != null
                && accountRepository.findByIban(transaction.getToIban())
                        .map(a -> a.getUser().getId() == customer.getId())
                        .orElse(false);
        if (!ownsFromIban && !ownsToIban) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may not view this transaction");
        }
    }

    @Transactional
    public Transaction create(TransactionCreateRequest request, User initiatedBy) {
        TransactionType type = request.type();

        if (type == TransactionType.TRANSFER) {
            Account fromAccount = findAccount(request.fromIban(), "Source");
            Account toAccount = findAccount(request.toIban(), "Destination");
            BigDecimal outgoingToday = computeOutgoingToday(fromAccount.getIban());
            rules.validateTransfer(request, fromAccount, toAccount, initiatedBy, outgoingToday);
            debit(fromAccount, request.amount());
            credit(toAccount, request.amount());

        } else if (type == TransactionType.DEPOSIT) {
            Account toAccount = findAccount(request.toIban(), "Destination");
            rules.validateDeposit(request, toAccount);
            credit(toAccount, request.amount());

        } else if (type == TransactionType.WITHDRAWAL) {
            Account fromAccount = findAccount(request.fromIban(), "Source");
            BigDecimal outgoingToday = computeOutgoingToday(fromAccount.getIban());
            rules.validateWithdrawal(request, fromAccount, initiatedBy, outgoingToday);
            debit(fromAccount, request.amount());

        } else {
            rules.rejectUnsupportedType(type);
        }

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setTimestamp(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    private BigDecimal computeOutgoingToday(String iban) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return transactionRepository
                .findByFromIbanAndTypeInAndTimestampGreaterThanEqual(iban, OUTGOING_LIMIT_TYPES, startOfDay)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Account findAccount(String iban, String label) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("No " + label.toLowerCase() + " account for IBAN " + iban));
    }

    private void debit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

    private void credit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }
}

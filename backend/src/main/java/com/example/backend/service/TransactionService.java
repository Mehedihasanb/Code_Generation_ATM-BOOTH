package com.example.backend.service;

import com.example.backend.domain.BankAccount;
import com.example.backend.domain.Transaction;
import com.example.backend.domain.UserRegistration;
import com.example.backend.policy.TransferAuthorizationPolicy;
import com.example.backend.support.AtmConstants;
import com.example.backend.dto.TransferRequest;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.TransactionRepository;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.backend.dto.TransactionHistoryRow;
import com.example.backend.dto.SystemTransactionRow;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TransactionService {

        private final BankAccountRepository bankAccountRepository;
        private final TransactionRepository transactionRepository;
        private final UserRegistrationRepository userRegistrationRepository;
        private final TransferAuthorizationPolicy transferAuthorizationPolicy;

        public TransactionService(
                        BankAccountRepository bankAccountRepository,
                        TransactionRepository transactionRepository,
                        UserRegistrationRepository userRegistrationRepository,
                        TransferAuthorizationPolicy transferAuthorizationPolicy) {
                this.bankAccountRepository = bankAccountRepository;
                this.transactionRepository = transactionRepository;
                this.userRegistrationRepository = userRegistrationRepository;
                this.transferAuthorizationPolicy = transferAuthorizationPolicy;
        }

        @Transactional
        public void processTransfer(TransferRequest request, Authentication authentication) {
                String userEmail = authentication.getName();
                boolean isEmployee = isEmployee(authentication);

                UserRegistration initiator = userRegistrationRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                BankAccount fromAccount = bankAccountRepository.findByIban(request.fromIban())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sender account not found"));

                BankAccount toAccount = bankAccountRepository.findByIban(request.toIban())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Receiver account not found"));

                // Authorization Check
                if (isEmployee) {
                        // Requirement: Employees can only transfer between checking accounts
                        if (!"CHECKING".equalsIgnoreCase(fromAccount.getAccountType().name()) ||
                                        !"CHECKING".equalsIgnoreCase(toAccount.getAccountType().name())) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Employees can only transfer funds between CHECKING accounts.");
                        }
                } else {
                        // Requirement: Customers must own the account they are sending from
                        transferAuthorizationPolicy.requireCanInitiateFromAccount(initiator, fromAccount);
                }

                // Verify accounts are active
                transferAuthorizationPolicy.requireActiveAccounts(fromAccount, toAccount);

                // Enforcement: Sender cannot go below zero
                BigDecimal newBalance = fromAccount.getBalance().subtract(request.amount());
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Insufficient funds. Maximum available to transfer is €"
                                                        + formatAmount(fromAccount.getBalance()));
                }

                // Enforcement: Daily limit
                java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
                BigDecimal totalTransferredToday = transactionRepository.sumOutgoingTransactionsToday(fromAccount,
                                startOfDay);
                BigDecimal projectedTotal = totalTransferredToday.add(request.amount());

                if (projectedTotal.compareTo(fromAccount.getDailyOutgoingTransferLimit()) > 0) {
                        BigDecimal remainingDailyLimit = fromAccount.getDailyOutgoingTransferLimit()
                                        .subtract(totalTransferredToday);
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Transfer exceeds the daily limit. Only €"
                                                        + formatAmount(remainingDailyLimit)
                                                        + " more can be transferred today.");
                }

                // Enforcement: Receiver cannot exceed their AML cap
                BigDecimal projectedReceiverBalance = toAccount.getBalance().add(request.amount());
                if (projectedReceiverBalance.compareTo(toAccount.getMinimumAllowedBalance()) > 0) {
                        BigDecimal remainingSpace = toAccount.getMinimumAllowedBalance()
                                        .subtract(toAccount.getBalance());
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Transfer failed. The receiving account has a strict maximum balance cap and can only accept €"
                                                        + formatAmount(remainingSpace) + " more.");
                }

                fromAccount.setBalance(newBalance);
                toAccount.setBalance(projectedReceiverBalance);

                bankAccountRepository.save(fromAccount);
                bankAccountRepository.save(toAccount);

                Transaction transaction = new Transaction(
                                fromAccount,
                                toAccount,
                                request.amount(),
                                request.description(),
                                initiator); // Records the exact employee who forced the transfer
                transactionRepository.save(transaction);
        }

        @Transactional(readOnly = true)
        public Page<?> getTransactions(
                        String accountIban, LocalDate startDate, LocalDate endDate,
                        BigDecimal amount, String amountOperator, String counterpartIban,
                        Authentication authentication, Pageable pageable) {

                boolean isEmployee = isEmployee(authentication);

                if (isEmployee && accountIban == null) {
                        return getAllSystemTransactions(pageable);
                }
                if (accountIban == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "accountIban is required for customer queries.");
                }

                return getTransactionHistory(
                                accountIban, startDate, endDate, amount, amountOperator, counterpartIban,
                                authentication.getName(), pageable);
        }

        private String formatAmount(BigDecimal amount) {
                if (amount == null)
                        return "0.00";
                return amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString();

        }

        @Transactional(readOnly = true)
        public Page<TransactionHistoryRow> getTransactionHistory(
                        String iban, LocalDate startDate, LocalDate endDate,
                        BigDecimal amount, String amountOperator, String counterpartIban,
                        String userEmail, Pageable pageable) {

                BankAccount account = bankAccountRepository.findByIban(iban)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Account not found"));

                if (!account.getOwner().getEmail().equals(userEmail)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission.");
                }
                LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
                LocalDateTime end = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

                BigDecimal exactAmount = "eq".equalsIgnoreCase(amountOperator) ? amount : null;
                BigDecimal minAmount = "gt".equalsIgnoreCase(amountOperator) ? amount : null;
                BigDecimal maxAmount = "lt".equalsIgnoreCase(amountOperator) ? amount : null;

                Page<Transaction> transactions = transactionRepository.findWithFilters(
                                iban, counterpartIban, start, end, exactAmount, minAmount, maxAmount, pageable);

                return transactions.map(tx -> {
                        boolean isOutgoing = tx.getFromAccount().getIban().equals(iban);
                        String type = isOutgoing ? "OUTGOING" : "INCOMING";
                        String counterpart = isOutgoing ? tx.getToAccount().getIban() : tx.getFromAccount().getIban();

                        return new TransactionHistoryRow(
                                        tx.getId(),
                                        tx.getTimestamp(),
                                        tx.getAmount(),
                                        counterpart,
                                        type,
                                        tx.getDescription());
                });
        }

        @Transactional(readOnly = true)
        public Page<SystemTransactionRow> getAllSystemTransactions(Pageable pageable) {
                Page<Transaction> transactions = transactionRepository.findAll(pageable);

                return transactions.map(this::mapToSystemTransactionRow);
        }

        @Transactional(readOnly = true)
        public Page<SystemTransactionRow> getTransactionsByUserId(Long userId, Pageable pageable) {
                Page<Transaction> transactions = transactionRepository.findAllByUserId(userId, pageable);
                return transactions.map(this::mapToSystemTransactionRow);
        }

        private SystemTransactionRow mapToSystemTransactionRow(Transaction tx) {
                String fromIban = tx.getFromAccount() != null ? tx.getFromAccount().getIban() : "SYSTEM/ATM";
                String toIban = tx.getToAccount() != null ? tx.getToAccount().getIban() : "SYSTEM/ATM";
                String initiatingUser = tx.getInitiatingUser() != null ? tx.getInitiatingUser().getEmail() : "System";

                String type;
                if (tx.getFromAccount() == null || tx.getToAccount() == null
                                || isAtmTransaction(tx)) {
                        type = "ATM";
                } else if (tx.getInitiatingUser() != null && tx.getInitiatingUser().getRole().contains("EMPLOYEE")) {
                        type = "EMPLOYEE";
                } else {
                        type = "TRANSFER";
                }

                return new SystemTransactionRow(
                                tx.getId(),
                                tx.getTimestamp(),
                                fromIban,
                                toIban,
                                tx.getAmount(),
                                initiatingUser,
                                type);
        }

        private boolean isAtmTransaction(Transaction tx) {
                return AtmConstants.WITHDRAWAL_DESCRIPTION.equals(tx.getDescription())
                                || AtmConstants.DEPOSIT_DESCRIPTION.equals(tx.getDescription())
                                || (tx.getToAccount() != null
                                                && AtmConstants.SYSTEM_ATM_IBAN.equals(tx.getToAccount().getIban()))
                                || (tx.getFromAccount() != null
                                                && AtmConstants.SYSTEM_ATM_IBAN.equals(tx.getFromAccount().getIban()));
        }

        private boolean isEmployee(Authentication authentication) {
                return authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .anyMatch(role -> role.equals("ROLE_EMPLOYEE"));
        }
}
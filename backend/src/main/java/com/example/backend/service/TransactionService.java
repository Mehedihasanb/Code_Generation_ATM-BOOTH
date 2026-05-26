package com.example.backend.service;

import com.example.backend.domain.BankAccount;
import com.example.backend.domain.Transaction;
import com.example.backend.domain.UserRegistration;
import com.example.backend.policy.TransferAuthorizationPolicy;
import com.example.backend.dto.TransferRequest;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.TransactionRepository;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.backend.dto.TransactionHistoryRow;

import java.math.BigDecimal;

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
        public void processTransfer(TransferRequest request, String userEmail) {
                UserRegistration initiator = userRegistrationRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                BankAccount fromAccount = bankAccountRepository.findByIban(request.fromIban())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sender account not found"));

                BankAccount toAccount = bankAccountRepository.findByIban(request.toIban())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Receiver account not found"));

                transferAuthorizationPolicy.requireCanInitiateFromAccount(initiator, fromAccount);
                transferAuthorizationPolicy.requireActiveAccounts(fromAccount, toAccount);

                // Enforcement: Sender cannot go below zero
                BigDecimal newBalance = fromAccount.getBalance().subtract(request.amount());
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Insufficient funds. Maximum available to transfer is €"
                                                        + fromAccount.getBalance());
                }

                // Enforcement: The daily limit
                java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
                BigDecimal totalTransferredToday = transactionRepository.sumOutgoingTransactionsToday(fromAccount,
                                startOfDay);
                BigDecimal projectedTotal = totalTransferredToday.add(request.amount());

                if (projectedTotal.compareTo(fromAccount.getDailyOutgoingTransferLimit()) > 0) {
                        BigDecimal remainingDailyLimit = fromAccount.getDailyOutgoingTransferLimit()
                                        .subtract(totalTransferredToday);
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Transfer exceeds your daily limit. You can only transfer €"
                                                        + remainingDailyLimit + " more today.");
                }

                // Enforcement: Receiver cannot exceed their AML cap
                BigDecimal projectedReceiverBalance = toAccount.getBalance().add(request.amount());
                if (projectedReceiverBalance.compareTo(toAccount.getMinimumAllowedBalance()) > 0) {
                        BigDecimal remainingSpace = toAccount.getMinimumAllowedBalance()
                                        .subtract(toAccount.getBalance());
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Transfer failed. The receiving account has a strict maximum balance cap and can only accept €"
                                                        + remainingSpace + " more.");
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
                                initiator);
                transactionRepository.save(transaction);
        }

        @Transactional(readOnly = true)
        public Page<TransactionHistoryRow> getTransactionHistory(String iban, String userEmail, Pageable pageable) {
                // Find account and verify ownership
                BankAccount account = bankAccountRepository.findByIban(iban)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Account not found"));

                if (!account.getOwner().getEmail().equals(userEmail)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "You do not have permission to view this account's history.");
                }

                // Fetch the paginated transactions
                Page<Transaction> transactions = transactionRepository
                                .findByFromAccount_IbanOrToAccount_IbanOrderByTimestampDesc(iban, iban, pageable);

                // Map to the DTO relative to the requested IBAN
                return transactions.map(tx -> {
                        boolean isOutgoing = tx.getFromAccount().getIban().equals(iban);

                        String type = isOutgoing ? "OUTGOING" : "INCOMING";
                        String counterpartIban = isOutgoing ? tx.getToAccount().getIban()
                                        : tx.getFromAccount().getIban();

                        return new TransactionHistoryRow(
                                        tx.getId(),
                                        tx.getTimestamp(),
                                        tx.getAmount(),
                                        counterpartIban,
                                        type,
                                        tx.getDescription());
                });
        }
}
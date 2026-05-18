package com.example.backend.service;

import com.example.backend.domain.BankAccount;
import com.example.backend.domain.Transaction;
import com.example.backend.domain.UserRegistration;
import com.example.backend.dto.TransferRequest;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.TransactionRepository;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRegistrationRepository userRegistrationRepository;

    public TransactionService(
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            UserRegistrationRepository userRegistrationRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.userRegistrationRepository = userRegistrationRepository;
    }

    @Transactional
    public void processTransfer(TransferRequest request, String userEmail) {
        UserRegistration initiator = userRegistrationRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        BankAccount fromAccount = bankAccountRepository.findByIban(request.fromIban())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender account not found"));

        BankAccount toAccount = bankAccountRepository.findByIban(request.toIban())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver account not found"));

        if (!fromAccount.getOwner().getId().equals(initiator.getId()) && !"EMPLOYEE".equals(initiator.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to transfer from this account.");
        }

        if (!fromAccount.isActive() || !toAccount.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or both accounts are inactive.");
        }

        BigDecimal newBalance = fromAccount.getBalance().subtract(request.amount());
        if (newBalance.compareTo(fromAccount.getMinimumAllowedBalance()) < 0) {
            BigDecimal availableFunds = fromAccount.getBalance().subtract(fromAccount.getMinimumAllowedBalance());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transfer exceeds absolute limit. Maximum available to transfer: €" + availableFunds);
        }

        fromAccount.setBalance(newBalance);
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));

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
}
package com.example.backend.dtos;

import com.example.backend.entities.enums.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionFilterParams {

    private Integer customerId;
    private String iban;
    private String accountIban;
    private String counterpartIban;
    private TransactionType type;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal amount;
    private String amountOperator;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getAccountIban() { return accountIban; }
    public void setAccountIban(String accountIban) { this.accountIban = accountIban; }

    public String getCounterpartIban() { return counterpartIban; }
    public void setCounterpartIban(String counterpartIban) { this.counterpartIban = counterpartIban; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getAmountOperator() { return amountOperator; }
    public void setAmountOperator(String amountOperator) { this.amountOperator = amountOperator; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}

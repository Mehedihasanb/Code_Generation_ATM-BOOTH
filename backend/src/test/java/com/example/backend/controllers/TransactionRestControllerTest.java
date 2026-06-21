package com.example.backend.controllers;

import com.example.backend.entities.Account;
import com.example.backend.entities.CustomerProfile;
import com.example.backend.entities.Transaction;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.AccountStatus;
import com.example.backend.entities.enums.AccountType;
import com.example.backend.entities.enums.CustomerStatus;
import com.example.backend.entities.enums.TransactionType;
import com.example.backend.entities.enums.UserRole;
import com.example.backend.repositories.AccountRepository;
import com.example.backend.repositories.CustomerProfileRepository;
import com.example.backend.repositories.TransactionRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.util.JwtUtil;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // rolls back DB changes after each test
@DisplayName("Transaction REST endpoints")
class TransactionRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;


    private User createCustomer(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setFirstName("Test");
        user.setLastName("Customer");
        user.setRole(UserRole.CUSTOMER);
        User saved = userRepository.save(user);
        createProfile(saved, CustomerStatus.ACTIVE);
        return saved;
    }

    private CustomerProfile createProfile(User user, CustomerStatus status) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setBsn(String.format("%09d", user.getId()));
        profile.setPhoneNumber("0612345678");
        profile.setStatus(status);
        return customerProfileRepository.save(profile);
    }

    private User createEmployee(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setFirstName("Test");
        user.setLastName("Employee");
        user.setRole(UserRole.EMPLOYEE);
        return userRepository.save(user);
    }

    private Account createAccount(User owner, String iban) {
        return createAccount(owner, iban, AccountType.CHECKING);
    }

    private Account createAccount(User owner, String iban, AccountType type) {
        Account account = new Account();
        account.setUser(owner);
        account.setIban(iban);
        account.setType(type);
        account.setBalance(new BigDecimal("1000.00"));
        account.setMinimumBalanceLimit(new BigDecimal("-500.00"));
        account.setDailyTransferLimit(new BigDecimal("5000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    private Transaction createTransaction(User initiatedBy, String fromIban, String toIban,
                                          TransactionType type, BigDecimal amount) {
        return createTransaction(initiatedBy, fromIban, toIban, type, amount, LocalDateTime.now());
    }

    private Transaction createTransaction(User initiatedBy, String fromIban, String toIban,
                                          TransactionType type, BigDecimal amount,
                                          LocalDateTime timestamp) {
        Transaction transaction = new Transaction();
        transaction.setInitiatedBy(initiatedBy);
        transaction.setFromIban(fromIban);
        transaction.setToIban(toIban);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setTimestamp(timestamp);
        return transactionRepository.save(transaction);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }





    @Nested
    @DisplayName("POST /transactions")
    class SubmitTransaction {

    @Test
    void submit_deposit_returns201() throws Exception {
        User customer = createCustomer("txn-deposit@test.inholland.nl");
        Account toAccount = createAccount(customer, "txn-IBAN-DEPOSIT-01");

        Map<String, Object> request = new HashMap<>();
        request.put("toIban", toAccount.getIban());
        request.put("amount", "100.00");
        request.put("type", "DEPOSIT");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.toIban").value(toAccount.getIban()))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.id").isNumber());
    }



    @Test
    void submit_missingType_returns400() throws Exception {
        User customer = createCustomer("txn-badrequest@test.inholland.nl");

        Map<String, Object> request = new HashMap<>();
        request.put("toIban", "txn-IBAN-ANY");
        request.put("amount", "50.00");
        // no type field — should fail validation

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void submit_whenPending_returns403() throws Exception {
        User customer = createCustomer("txn-pending@test.inholland.nl");
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.PENDING);
        customerProfileRepository.save(profile);
        Account toAccount = createAccount(customer, "txn-IBAN-PENDING-01");

        Map<String, Object> request = new HashMap<>();
        request.put("toIban", toAccount.getIban());
        request.put("amount", "100.00");
        request.put("type", "DEPOSIT");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }



    @Test
    void submit_whenClosed_returns403() throws Exception {
        User customer = createCustomer("txn-closed@test.inholland.nl");
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.CLOSED);
        customerProfileRepository.save(profile);
        Account toAccount = createAccount(customer, "txn-IBAN-CLOSED-01");

        Map<String, Object> request = new HashMap<>();
        request.put("toIban", toAccount.getIban());
        request.put("amount", "100.00");
        request.put("type", "DEPOSIT");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }



    @Test
    void submit_externalToSavings_returns400() throws Exception {
        User sender = createCustomer("txn-external-sender@test.inholland.nl");
        User recipient = createCustomer("txn-external-recipient@test.inholland.nl");
        Account fromAccount = createAccount(sender, "txn-IBAN-EXT-FROM");
        Account toSavingsAccount = createAccount(recipient, "txn-IBAN-EXT-SAV", AccountType.SAVINGS);

        Map<String, Object> request = new HashMap<>();
        request.put("fromIban", fromAccount.getIban());
        request.put("toIban", toSavingsAccount.getIban());
        request.put("amount", "100.00");
        request.put("type", "TRANSFER");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(sender))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void submit_unauthenticated_returns401() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("toIban", "txn-IBAN-ANY");
        request.put("amount", "50.00");
        request.put("type", "DEPOSIT");

        // no token = 401
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }


    }
    @Nested
    @DisplayName("GET /transactions/{id}")
    class TransactionDetail {

    @Test
    void detail_returnsOwnTransaction() throws Exception {
        User customer = createCustomer("txn-owner@test.inholland.nl");
        Transaction transaction = createTransaction(
                customer, null, "txn-IBAN-TO", TransactionType.DEPOSIT, new BigDecimal("75.00"));

        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId()))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(75.00));
    }



    @Test
    void detail_otherCustomer_returns403() throws Exception {
        User customer1 = createCustomer("txn-customer1@test.inholland.nl");
        User customer2 = createCustomer("txn-customer2@test.inholland.nl");
        Transaction transaction = createTransaction(
                customer1, null, "txn-IBAN-C1", TransactionType.DEPOSIT, new BigDecimal("50.00"));

        // customer2 tries to view customer1's transaction — should be denied.
        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(customer2)))
                .andExpect(status().isForbidden());
    }



    @Test
    void detail_asEmployee_returnsAnyTransaction() throws Exception {
        User customer = createCustomer("txn-customer-emp@test.inholland.nl");
        User employee = createEmployee("txn-employee@test.inholland.nl");
        Transaction transaction = createTransaction(
                customer, null, "txn-IBAN-EMP", TransactionType.DEPOSIT, new BigDecimal("60.00"));

        // Employees can view any transaction regardless of who initiated it.
        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId()));
    }



    @Test
    void detail_whenPending_returns403() throws Exception {
        User customer = createCustomer("txn-detail-pending@test.inholland.nl");
        Transaction transaction = createTransaction(
                customer, null, "txn-IBAN-PENDING-DETAIL", TransactionType.DEPOSIT, new BigDecimal("60.00"));
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.PENDING);
        customerProfileRepository.save(profile);

        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }



    @Test
    void detail_whenClosed_returns403() throws Exception {
        User customer = createCustomer("txn-detail-closed@test.inholland.nl");
        Transaction transaction = createTransaction(
                customer, null, "txn-IBAN-CLOSED-DETAIL", TransactionType.DEPOSIT, new BigDecimal("60.00"));
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.CLOSED);
        customerProfileRepository.save(profile);

        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }



    @Test
    void detail_recipientCanViewIncomingTransfer() throws Exception {
        User alice = createCustomer("txn-alice-detail@test.inholland.nl");
        User charlie = createCustomer("txn-charlie-detail@test.inholland.nl");
        Account aliceAccount = createAccount(alice, "txn-IBAN-ALICE-DETAIL");
        Account charlieAccount = createAccount(charlie, "txn-IBAN-CHARLIE-DETAIL");

        Transaction transfer = createTransaction(alice,
                aliceAccount.getIban(), charlieAccount.getIban(),
                TransactionType.TRANSFER, new BigDecimal("50.00"));

        mockMvc.perform(get("/transactions/{id}", transfer.getId())
                        .header("Authorization", bearerToken(charlie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transfer.getId()));
    }


    }
    @Nested
    @DisplayName("GET /transactions")
    class TransactionHistory {

    @Test
    void history_returnsOnlyOwnRows() throws Exception {
        User customer1 = createCustomer("txn-filter-c1@test.inholland.nl");
        User customer2 = createCustomer("txn-filter-c2@test.inholland.nl");

        createTransaction(customer1, null, "txn-IBAN-FC1", TransactionType.DEPOSIT, new BigDecimal("100.00"));
        createTransaction(customer2, null, "txn-IBAN-FC2", TransactionType.DEPOSIT, new BigDecimal("200.00"));

        // customers can only see their own transactions
        mockMvc.perform(get("/transactions")
                        .header("Authorization", bearerToken(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].initiatedByUserId",
                        everyItem(is(customer1.getId()))));
    }



    @Test
    void history_filtersInclusiveDateRange() throws Exception {
        User customer = createCustomer("txn-date-filter@test.inholland.nl");
        Transaction before = createTransaction(customer, null, "txn-IBAN-DATE-OLD",
                TransactionType.DEPOSIT, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 5, 1, 23, 59));
        Transaction firstDay = createTransaction(customer, null, "txn-IBAN-DATE-START",
                TransactionType.DEPOSIT, new BigDecimal("200.00"),
                LocalDateTime.of(2026, 5, 2, 0, 0));
        Transaction lastDay = createTransaction(customer, null, "txn-IBAN-DATE-END",
                TransactionType.DEPOSIT, new BigDecimal("300.00"),
                LocalDateTime.of(2026, 5, 3, 23, 59));
        Transaction after = createTransaction(customer, null, "txn-IBAN-DATE-NEW",
                TransactionType.DEPOSIT, new BigDecimal("400.00"),
                LocalDateTime.of(2026, 5, 4, 0, 0));

        mockMvc.perform(get("/transactions")
                        .param("startDate", "2026-05-02")
                        .param("endDate", "2026-05-03")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + firstDay.getId() + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + lastDay.getId() + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + before.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id == " + after.getId() + ")]").doesNotExist());
    }



    @Test
    void history_invalidDateRange_returns400() throws Exception {
        User customer = createCustomer("txn-date-filter-invalid@test.inholland.nl");

        mockMvc.perform(get("/transactions")
                        .param("startDate", "2026-05-04")
                        .param("endDate", "2026-05-03")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void history_recipientSeesIncomingTransfer() throws Exception {
        // Alice sends money to Charlie. Charlie's account is the toIban.
        // Before the fix, Charlie could not see this transaction because he didn't initiate it.
        User alice = createCustomer("txn-alice@test.inholland.nl");
        User charlie = createCustomer("txn-charlie@test.inholland.nl");
        Account aliceAccount = createAccount(alice, "txn-IBAN-ALICE");
        Account charlieAccount = createAccount(charlie, "txn-IBAN-CHARLIE");

        Transaction transfer = createTransaction(alice,
                aliceAccount.getIban(), charlieAccount.getIban(),
                TransactionType.TRANSFER, new BigDecimal("50.00"));

        // Charlie must see the transfer in his own history
        mockMvc.perform(get("/transactions")
                        .header("Authorization", bearerToken(charlie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + transfer.getId() + ")]").exists());
    }



    @Test
    void history_whenPending_returns403() throws Exception {
        User customer = createCustomer("txn-history-pending@test.inholland.nl");
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.PENDING);
        customerProfileRepository.save(profile);

        mockMvc.perform(get("/transactions")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    }
}

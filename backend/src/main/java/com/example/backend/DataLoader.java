package com.example.backend;

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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements ApplicationRunner {

    // --- Seeded credentials (for developer reference) -------------------------
    // employee@inholland.nl   / Password123!
    // customer@inholland.nl   / Password123!
    // alice@inholland.nl      / Password123!
    // bob@inholland.nl        / Password123!
    // carol@inholland.nl      / Password123!
    // frank@inholland.nl      / Password123!  (zero-balance demo)
    // dave@inholland.nl       / Password123!  (pending)
    // eva@inholland.nl        / Password123!  (pending)
    // --------------------------------------------------------------------------

    private static final String DEMO_PASSWORD = "Password123!";

    // Columns: firstName, lastName, email, BSN, phone, checkingIban, savingsIban, checkingBalance, savingsBalance
    private static final String[][] EXTRA_CUSTOMER_DATA = {
        {"Alice", "Bakker",   "alice@inholland.nl",  "222222222", "0612345679", "NL22INHO0000000003", "NL22INHO0000000004", "2500.00", "8000.00"},
        {"Carol", "Jansen",   "carol@inholland.nl",  "444444444", "0612345681", "NL44INHO0000000007", "NL44INHO0000000008", "3200.00", "11000.00"},
        {"Frank", "Zero",     "frank@inholland.nl",  "777777777", "0612345682", "NL55INHO0000000009", "NL55INHO0000000010", "0.00",    "0.00"},
    };

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository,
                      CustomerProfileRepository customerProfileRepository,
                      AccountRepository accountRepository,
                      TransactionRepository transactionRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        // ------------------------------------------------------------------ //
        //  1. EMPLOYEE                                                        //
        // ------------------------------------------------------------------ //
        User employee = new User(0, "employee@inholland.nl",
                passwordEncoder.encode(DEMO_PASSWORD),
                "Employee", "User", UserRole.EMPLOYEE, LocalDateTime.now());
        userRepository.save(employee);

        // ------------------------------------------------------------------ //
        //  2. CUSTOMER  — primary demo account / bulk-transfer initiator      //
        // ------------------------------------------------------------------ //
        User customer = new User(0, "customer@inholland.nl",
                passwordEncoder.encode(DEMO_PASSWORD),
                "Customer", "User", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(customer);
        customerProfileRepository.save(
                new CustomerProfile(0, customer, "123456789", "0612345678", CustomerStatus.ACTIVE));

        BigDecimal customerCheckingBalance = new BigDecimal("4000.00");
        Account customerChecking = accountRepository.save(new Account(0, customer,
                "NL11INHO0000000001", AccountType.CHECKING,
                customerCheckingBalance,
                new BigDecimal("0.00"),
                customerCheckingBalance.multiply(new BigDecimal("0.10")),
                AccountStatus.ACTIVE, LocalDateTime.now()));

        BigDecimal customerSavingsBalance = new BigDecimal("9000.00");
        Account customerSavings = accountRepository.save(new Account(0, customer,
                "NL11INHO0000000002", AccountType.SAVINGS,
                customerSavingsBalance,
                new BigDecimal("0.00"),
                customerSavingsBalance.multiply(new BigDecimal("0.10")),
                AccountStatus.ACTIVE, LocalDateTime.now()));

        // ------------------------------------------------------------------ //
        //  3. BOB  — active customer                                          //
        // ------------------------------------------------------------------ //
        User bob = new User(0, "bob@inholland.nl",
                passwordEncoder.encode(DEMO_PASSWORD),
                "Bob", "de Vries", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(bob);
        customerProfileRepository.save(
                new CustomerProfile(0, bob, "333333333", "0612345680", CustomerStatus.ACTIVE));

        BigDecimal bobCheckingBalance = new BigDecimal("1800.00");
        Account bobChecking = accountRepository.save(new Account(0, bob,
                "NL33INHO0000000005", AccountType.CHECKING,
                bobCheckingBalance,
                new BigDecimal("0.00"),
                bobCheckingBalance.multiply(new BigDecimal("0.10")),
                AccountStatus.ACTIVE, LocalDateTime.now()));

        accountRepository.save(new Account(0, bob,
                "NL33INHO0000000006", AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("500.00"),
                AccountStatus.ACTIVE, LocalDateTime.now()));

        // ------------------------------------------------------------------ //
        //  4. PENDING CUSTOMERS (no accounts, cannot transact)                //
        // ------------------------------------------------------------------ //
        User dave = new User(0, "dave@inholland.nl",
                passwordEncoder.encode(DEMO_PASSWORD),
                "Dave", "Pending", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(dave);
        customerProfileRepository.save(
                new CustomerProfile(0, dave, "555555555", "0612345683", CustomerStatus.PENDING));

        User eva = new User(0, "eva@inholland.nl",
                passwordEncoder.encode(DEMO_PASSWORD),
                "Eva", "Wachtend", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(eva);
        customerProfileRepository.save(
                new CustomerProfile(0, eva, "666666666", "0612345684", CustomerStatus.PENDING));

        // ------------------------------------------------------------------ //
        //  5. SAMPLE TRANSACTIONS                                               //
        // ------------------------------------------------------------------ //
        transactionRepository.save(new Transaction(0,
                customerChecking.getIban(), bobChecking.getIban(),
                customer, new BigDecimal("250.00"), TransactionType.TRANSFER,
                "Rent payment", LocalDateTime.now().minusHours(26)));

        transactionRepository.save(new Transaction(0,
                null, customerSavings.getIban(),
                customer, new BigDecimal("500.00"), TransactionType.DEPOSIT,
                "Initial deposit", LocalDateTime.now().minusHours(27)));

        // ------------------------------------------------------------------ //
        //  6. EXTRA ACTIVE CUSTOMERS — fixed IBANs from the RhinoBank demo    //
        // ------------------------------------------------------------------ //
        List<Account> extraCheckingAccounts = new ArrayList<>();

        for (String[] row : EXTRA_CUSTOMER_DATA) {
            String firstName = row[0];
            String lastName = row[1];
            String email = row[2];
            String bsn = row[3];
            String phone = row[4];
            String checkingIban = row[5];
            String savingsIban = row[6];
            BigDecimal checkingBalance = new BigDecimal(row[7]);
            BigDecimal savingsBalance = new BigDecimal(row[8]);

            User user = new User(0, email,
                    passwordEncoder.encode(DEMO_PASSWORD),
                    firstName, lastName, UserRole.CUSTOMER, LocalDateTime.now());
            userRepository.save(user);
            customerProfileRepository.save(
                    new CustomerProfile(0, user, bsn, phone, CustomerStatus.ACTIVE));

            Account checking = accountRepository.save(new Account(0, user, checkingIban,
                    AccountType.CHECKING,
                    checkingBalance,
                    new BigDecimal("0.00"),
                    checkingBalance.multiply(new BigDecimal("0.10")),
                    AccountStatus.ACTIVE, LocalDateTime.now()));
            extraCheckingAccounts.add(checking);

            accountRepository.save(new Account(0, user, savingsIban,
                    AccountType.SAVINGS,
                    savingsBalance,
                    new BigDecimal("0.00"),
                    savingsBalance.multiply(new BigDecimal("0.10")),
                    AccountStatus.ACTIVE, LocalDateTime.now()));
        }

        // ------------------------------------------------------------------ //
        //  7. BULK TRANSFERS FROM CUSTOMER → EACH EXTRA CHECKING ACCOUNT      //
        // ------------------------------------------------------------------ //
        String[] descriptions = {"Groceries", "Rent share", "Dinner", "Concert tickets", "Books", "Gift"};

        for (int i = 0; i < extraCheckingAccounts.size(); i++) {
            transactionRepository.save(new Transaction(0,
                    customerChecking.getIban(),
                    extraCheckingAccounts.get(i).getIban(),
                    customer,
                    new BigDecimal("50.00"),
                    TransactionType.TRANSFER,
                    "Transfer to " + EXTRA_CUSTOMER_DATA[i][0] + " " + EXTRA_CUSTOMER_DATA[i][1],
                    LocalDateTime.now().minusHours(i)));
        }

        // ------------------------------------------------------------------ //
        //  8. DEMO TRANSACTION HISTORY (paginated employee directory view)    //
        // ------------------------------------------------------------------ //
        List<Account> historyAccounts = List.of(customerChecking, bobChecking,
                extraCheckingAccounts.get(0), extraCheckingAccounts.get(1));
        List<User> historyInitiators = List.of(customer, bob,
                userRepository.findByEmail("alice@inholland.nl").orElse(customer),
                userRepository.findByEmail("carol@inholland.nl").orElse(customer));

        for (int i = 0; i < 19; i++) {
            Account from = historyAccounts.get(i % historyAccounts.size());
            Account to = historyAccounts.get((i + 1) % historyAccounts.size());
            transactionRepository.save(new Transaction(0,
                    from.getIban(),
                    to.getIban(),
                    historyInitiators.get(i % historyInitiators.size()),
                    BigDecimal.valueOf(15 + (i * 5L)),
                    TransactionType.TRANSFER,
                    descriptions[i % descriptions.length],
                    LocalDateTime.now().minusDays(i % 14).minusHours(i % 6)));
        }
    }
}

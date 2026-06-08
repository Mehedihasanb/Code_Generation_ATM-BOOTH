package com.example.backend.config;

import com.example.backend.domain.AccountType;
import com.example.backend.domain.BankAccount;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.Transaction;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.TransactionRepository;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

	// minimumAllowedBalance is the max balance cap (absolute limit) on each account
	private static final BigDecimal ABSOLUTE_LIMIT = new BigDecimal("50000.00");
	private static final BigDecimal DAILY_LIMIT = new BigDecimal("2000.00");

	// runs first so the employee + customers exist before other seeders (e.g. ATM system account)
	@Order(1)
	@Bean
	CommandLineRunner seedData(
			UserRegistrationRepository userRegistrationRepository,
			BankAccountRepository bankAccountRepository,
			TransactionRepository transactionRepository,
			PasswordEncoder passwordEncoder) {
		return startupArguments -> {
			ensureDemoEmployee(userRegistrationRepository, passwordEncoder);

			// only seed the bulk demo data once
			if (userRegistrationRepository.findByEmail("alice@inholland.nl").isPresent()) {
				ensureDemoPendingCustomer(userRegistrationRepository, bankAccountRepository, passwordEncoder,
					"Dave", "Pending", "dave@inholland.nl", "555555555");
				ensureDemoPendingCustomer(userRegistrationRepository, bankAccountRepository, passwordEncoder,
					"Eva", "Wachtend", "eva@inholland.nl", "666666666");
				ensureDemoApprovedCustomer(userRegistrationRepository, bankAccountRepository,
					"customer@inholland.nl");
				ensureZeroBalanceDemoCustomer(userRegistrationRepository, bankAccountRepository, passwordEncoder);
				return;
			}

			// approved customers with checking + savings accounts
			UserRegistration demoCustomer = approvedCustomer(userRegistrationRepository, passwordEncoder,
				"Customer", "User", "customer@inholland.nl", "123456789");
			UserRegistration alice = approvedCustomer(userRegistrationRepository, passwordEncoder,
				"Alice", "Bakker", "alice@inholland.nl", "222222222");
			UserRegistration bob = approvedCustomer(userRegistrationRepository, passwordEncoder,
				"Bob", "de Vries", "bob@inholland.nl", "333333333");
			UserRegistration carol = approvedCustomer(userRegistrationRepository, passwordEncoder,
				"Carol", "Jansen", "carol@inholland.nl", "444444444");
			UserRegistration frank = approvedCustomer(userRegistrationRepository, passwordEncoder,
				"Frank", "Zero", "frank@inholland.nl", "777777777");

			// pending customers (no accounts yet) for the live approve demo
			ensureDemoPendingCustomer(userRegistrationRepository, bankAccountRepository, passwordEncoder,
				"Dave", "Pending", "dave@inholland.nl", "555555555");
			ensureDemoPendingCustomer(userRegistrationRepository, bankAccountRepository, passwordEncoder,
				"Eva", "Wachtend", "eva@inholland.nl", "666666666");

			BankAccount custChecking = account(bankAccountRepository, demoCustomer, "NL11INHO0000000001", AccountType.CHECKING, "4000.00");
			account(bankAccountRepository, demoCustomer, "NL11INHO0000000002", AccountType.SAVINGS, "9000.00");
			BankAccount aliceChecking = account(bankAccountRepository, alice, "NL22INHO0000000003", AccountType.CHECKING, "2500.00");
			account(bankAccountRepository, alice, "NL22INHO0000000004", AccountType.SAVINGS, "8000.00");
			BankAccount bobChecking = account(bankAccountRepository, bob, "NL33INHO0000000005", AccountType.CHECKING, "1800.00");
			account(bankAccountRepository, bob, "NL33INHO0000000006", AccountType.SAVINGS, "5000.00");
			BankAccount carolChecking = account(bankAccountRepository, carol, "NL44INHO0000000007", AccountType.CHECKING, "3200.00");
			account(bankAccountRepository, carol, "NL44INHO0000000008", AccountType.SAVINGS, "11000.00");
			account(bankAccountRepository, frank, "NL55INHO0000000009", AccountType.CHECKING, "0.00");
			account(bankAccountRepository, frank, "NL55INHO0000000010", AccountType.SAVINGS, "0.00");

			// transactions between the checking accounts so the history list has enough rows to paginate
			List<BankAccount> checkings = new ArrayList<>();
			checkings.add(custChecking);
			checkings.add(aliceChecking);
			checkings.add(bobChecking);
			checkings.add(carolChecking);

			String[] descriptions = { "Groceries", "Rent share", "Dinner", "Concert tickets", "Books", "Gift" };

			for (int i = 0; i < 22; i++) {
				BankAccount from = checkings.get(i % checkings.size());
				BankAccount to = checkings.get((i + 1) % checkings.size());
				BigDecimal amount = new BigDecimal(15 + (i * 5)).setScale(2);
				String description = descriptions[i % descriptions.length];
				Transaction transaction = new Transaction(from, to, amount, description, from.getOwner());
				// spread the seeded transactions over the last ~2 weeks so date filtering is visible
				backdateTransaction(transaction, LocalDateTime.now().minusDays(i % 14).minusHours(i % 6));
				transactionRepository.save(transaction);
			}
		};
	}

	// Transaction stamps "now" in its constructor and has no setter, so we set the
	// seed date by reflection (demo data only, keeps the entity untouched)
	private void backdateTransaction(Transaction transaction, LocalDateTime when) {
		try {
			java.lang.reflect.Field timestampField = Transaction.class.getDeclaredField("timestamp");
			timestampField.setAccessible(true);
			timestampField.set(transaction, when);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not backdate seed transaction", exception);
		}
	}

	private UserRegistration approvedCustomer(UserRegistrationRepository repository, PasswordEncoder passwordEncoder,
			String firstName, String lastName, String email, String bsn) {
		return repository.findByEmail(email).orElseGet(() -> repository.save(new UserRegistration(
			firstName, lastName, email, passwordEncoder.encode("Password123!"), "CUSTOMER",
			CustomerApprovalStatus.APPROVED, bsn, "+31 6 12345678")));
	}

	private UserRegistration pendingCustomer(UserRegistrationRepository repository, PasswordEncoder passwordEncoder,
			String firstName, String lastName, String email, String bsn) {
		return repository.save(new UserRegistration(
			firstName, lastName, email, passwordEncoder.encode("Password123!"), "CUSTOMER",
			CustomerApprovalStatus.PENDING, bsn, "+31 6 12345678"));
	}

	// demo employee: create if missing, reactivate if you deactivated yourself during testing
	private void ensureDemoEmployee(UserRegistrationRepository repository, PasswordEncoder passwordEncoder) {
		var existing = repository.findByEmail("employee@inholland.nl");
		if (existing.isEmpty()) {
			repository.save(new UserRegistration(
					"Employee", "User", "employee@inholland.nl",
					passwordEncoder.encode("Password123!"), "EMPLOYEE", null, null, null));
			return;
		}
		UserRegistration employee = existing.get();
		if (employee.isDeleted()) {
			employee.setDeleted(false);
			repository.save(employee);
		}
	}

	// demo accounts: create dave/eva if missing, or reset to PENDING when denied/deleted but still no accounts
	private void ensureDemoPendingCustomer(
			UserRegistrationRepository userRepository,
			BankAccountRepository accountRepository,
			PasswordEncoder passwordEncoder,
			String firstName, String lastName, String email, String bsn) {
		var existing = userRepository.findByEmail(email);
		if (existing.isEmpty()) {
			pendingCustomer(userRepository, passwordEncoder, firstName, lastName, email, bsn);
			return;
		}
		UserRegistration user = existing.get();
		if (!accountRepository.findAllByOwner_Id(user.getId()).isEmpty()) {
			return;
		}
		user.setDeleted(false);
		user.setCustomerApprovalStatus(CustomerApprovalStatus.PENDING);
		userRepository.save(user);
	}

	// main demo customer - undo soft delete on restart so assessment login still works
	private void ensureDemoApprovedCustomer(
			UserRegistrationRepository userRepository,
			BankAccountRepository accountRepository,
			String email) {
		userRepository.findByEmail(email).ifPresent(user -> {
			if (!user.isDeleted()) {
				return;
			}
			user.setDeleted(false);
			userRepository.save(user);
			for (BankAccount account : accountRepository.findAllByOwner_Id(user.getId())) {
				account.setActive(true);
				accountRepository.save(account);
			}
		});
	}

	// zero balance on all accounts - use for permanent delete demo
	private void ensureZeroBalanceDemoCustomer(
			UserRegistrationRepository userRepository,
			BankAccountRepository accountRepository,
			PasswordEncoder passwordEncoder) {
		var existing = userRepository.findByEmail("frank@inholland.nl");
		if (existing.isEmpty()) {
			UserRegistration frank = approvedCustomer(userRepository, passwordEncoder,
					"Frank", "Zero", "frank@inholland.nl", "777777777");
			account(accountRepository, frank, "NL55INHO0000000009", AccountType.CHECKING, "0.00");
			account(accountRepository, frank, "NL55INHO0000000010", AccountType.SAVINGS, "0.00");
		}
	}

	private BankAccount account(BankAccountRepository repository, UserRegistration owner, String iban,
			AccountType type, String balance) {
		return repository.save(new BankAccount(
			owner, iban, type, true, new BigDecimal(balance).setScale(2), ABSOLUTE_LIMIT, DAILY_LIMIT));
	}
}

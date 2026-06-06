package com.example.backend.service;

import com.example.backend.domain.BankAccount;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.dto.CustomerAccountRow;
import com.example.backend.dto.CustomerDirectoryRow;
import com.example.backend.dto.PageResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerDirectoryService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final BankAccountRepository bankAccountRepository;

	public CustomerDirectoryService(UserRegistrationRepository userRegistrationRepository,
			BankAccountRepository bankAccountRepository) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.bankAccountRepository = bankAccountRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<CustomerDirectoryRow> listCustomerDirectory(Pageable pageable, Boolean hasAccountFilter,
			String firstName, String lastName, String iban) {
		if (hasSearchCriteria(firstName, lastName, iban)) {
			return searchCustomerDirectory(pageable, firstName, lastName, iban);
		}

		// Load customers from database
		Page<UserRegistration> customerPage;
		if (Boolean.FALSE.equals(hasAccountFilter)) {
			// US-09: employee sees pending customers with no bank account yet
			customerPage = userRegistrationRepository.findCustomersWithoutAccounts(pageable);
		} else {
			// US-12: employee sees all customers
			customerPage = userRegistrationRepository.findByRole("CUSTOMER", pageable);
		}

		List<UserRegistration> customers = customerPage.getContent();

		// Get customer ids on this page so we can load their accounts in one query
		List<Long> ids = new ArrayList<>();
		for (UserRegistration customer : customers) {
			ids.add(customer.getId());
		}

		List<BankAccount> accounts = new ArrayList<>();
		if (!ids.isEmpty()) {
			accounts = bankAccountRepository.findByOwner_IdIn(ids);
		}

		// Build response: one row per customer with their accounts
		List<CustomerDirectoryRow> rows = new ArrayList<>();
		for (UserRegistration customer : customers) {
			List<CustomerAccountRow> accountRows = new ArrayList<>();
			for (BankAccount account : accounts) {
				if (account.getOwner().getId().equals(customer.getId())) {
					accountRows.add(CustomerAccountRow.fromBankAccount(account));
				}
			}

			String approvalStatus = null;
			if (customer.getCustomerApprovalStatus() != null) {
				approvalStatus = customer.getCustomerApprovalStatus().name();
			}

			// Add customer + accounts to the page result
			rows.add(new CustomerDirectoryRow(
					customer.getId(),
					customer.getFirstName(),
					customer.getLastName(),
					customer.getEmail(),
					approvalStatus,
					accountRows));
		}

		// Wrap rows with pagination info (total pages, page number, etc.)
		return PageResponse.fromPage(customerPage, rows);
	}

	@Transactional(readOnly = true)
	public PageResponse<CustomerDirectoryRow> searchCustomerDirectory(Pageable pageable, String firstName,
			String lastName, String iban) {
		Map<Long, UserRegistration> matchedCustomers = new LinkedHashMap<>();

		if (hasText(firstName) && hasText(lastName)) {
			userRegistrationRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim())
					.forEach(user -> matchedCustomers.put(user.getId(), user));
		}

		if (hasText(iban)) {
			bankAccountRepository.findByIban(iban.trim()).ifPresent(account -> {
				UserRegistration owner = account.getOwner();
				if (owner != null && "CUSTOMER".equalsIgnoreCase(owner.getRole())) {
					matchedCustomers.put(owner.getId(), owner);
				}
			});
		}

		List<CustomerDirectoryRow> rows = matchedCustomers.values().stream()
				.map(this::toDirectoryRow)
				.toList();

		return PageResponse.fromPage(new PageImpl<>(rows, pageable, rows.size()), rows);
	}

	private boolean hasSearchCriteria(String firstName, String lastName, String iban) {
		return hasText(firstName) || hasText(lastName) || hasText(iban);
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private CustomerDirectoryRow toDirectoryRow(UserRegistration customer) {
		List<BankAccount> userAccounts = bankAccountRepository.findAllByOwner_Id(customer.getId());
		List<CustomerAccountRow> activeAccountRows = userAccounts.stream()
				.filter(BankAccount::isActive)
				.map(CustomerAccountRow::fromBankAccount)
				.toList();

		String approvalStatus = null;
		if (customer.getCustomerApprovalStatus() != null) {
			approvalStatus = customer.getCustomerApprovalStatus().name();
		}

		return new CustomerDirectoryRow(
				customer.getId(),
				customer.getFirstName(),
				customer.getLastName(),
				customer.getEmail(),
				approvalStatus,
				activeAccountRows);
	}
}

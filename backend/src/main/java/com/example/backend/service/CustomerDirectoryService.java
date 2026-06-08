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

// employee directory - US-09 pending list, US-12 all customers, search by name or iban
@Service
public class CustomerDirectoryService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final BankAccountRepository bankAccountRepository;

	public CustomerDirectoryService(UserRegistrationRepository userRegistrationRepository,
			BankAccountRepository bankAccountRepository) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.bankAccountRepository = bankAccountRepository;
	}

	// main entry - GET /users from CustomerDirectoryController
	@Transactional(readOnly = true)
	public PageResponse<CustomerDirectoryRow> listCustomerDirectory(Pageable pageable, Boolean hasAccountFilter,
			String firstName, String lastName, String iban) {
		// employee typed a search? skip the normal list and go to search method
		if (hasSearchCriteria(firstName, lastName, iban)) {
			return searchCustomerDirectory(pageable, firstName, lastName, iban);
		}

		Page<UserRegistration> customerPage;
		if (Boolean.FALSE.equals(hasAccountFilter)) {
			// US-09 service desk - pending customers who registered but have no accounts yet
			customerPage = userRegistrationRepository.findCustomersWithoutAccounts(pageable);
		} else {
			// US-12 directory page - all customers paginated (20 per page default)
			customerPage = userRegistrationRepository.findByRole("CUSTOMER", pageable);
		}

		List<UserRegistration> customers = customerPage.getContent();

		// page has e.g. 20 customers - collect their ids first
		List<Long> ids = new ArrayList<>();
		for (UserRegistration customer : customers) {
			ids.add(customer.getId());
		}

		// one db call for all accounts on this page (better than querying per customer)
		List<BankAccount> accounts = new ArrayList<>();
		if (!ids.isEmpty()) {
			accounts = bankAccountRepository.findByOwner_IdIn(ids);
		}

		// build one directory row per customer for the json response
		List<CustomerDirectoryRow> rows = new ArrayList<>();
		for (UserRegistration customer : customers) {
			List<CustomerAccountRow> accountRows = new ArrayList<>();
			// loop all accounts we fetched, keep the ones that belong to this customer
			for (BankAccount account : accounts) {
				if (account.getOwner().getId().equals(customer.getId())) {
					accountRows.add(CustomerAccountRow.fromBankAccount(account)); // entity -> dto
				}
			}

			// employees have null here, customers get PENDING/APPROVED/DENIED
			String approvalStatus = null;
			if (customer.getCustomerApprovalStatus() != null) {
				approvalStatus = customer.getCustomerApprovalStatus().name();
			}

			// pack customer info + their account list into one directory row
			rows.add(new CustomerDirectoryRow(
					customer.getId(),
					customer.getFirstName(),
					customer.getLastName(),
					customer.getEmail(),
					approvalStatus,
					accountRows));
		}

		// PageResponse wraps rows + page info (total pages, page number) for vue table
		return PageResponse.fromPage(customerPage, rows);
	}

	// search branch  by exact first+last name and/or iban
	@Transactional(readOnly = true)
	public PageResponse<CustomerDirectoryRow> searchCustomerDirectory(Pageable pageable, String firstName,
			String lastName, String iban) {
		// map by customer id - dedupes if name + iban search find the same person
		Map<Long, UserRegistration> matchedCustomers = new LinkedHashMap<>();

		// name search needs BOTH first and last - one alone is not enough
		if (hasText(firstName) && hasText(lastName)) {
			userRegistrationRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim())
					.forEach(user -> matchedCustomers.put(user.getId(), user));
		}

		// iban search - find account then grab the customer who owns it
		if (hasText(iban)) {
			bankAccountRepository.findByIban(iban.trim()).ifPresent(account -> {
				UserRegistration owner = account.getOwner();
				if (owner != null && "CUSTOMER".equalsIgnoreCase(owner.getRole())) {
					matchedCustomers.put(owner.getId(), owner); // skip if owner is employee somehow
				}
			});
		}

		// turn each matched customer into a directory dto row (active accounts only in helper)
		List<CustomerDirectoryRow> rows = matchedCustomers.values().stream()
				.map(this::toDirectoryRow)
				.toList();

		// search results usually small so we fake pagination with PageImpl + result size
		return PageResponse.fromPage(new PageImpl<>(rows, pageable, rows.size()), rows);
	}

	// true if employee typed anything in the search boxes
	private boolean hasSearchCriteria(String firstName, String lastName, String iban) {
		return hasText(firstName) || hasText(lastName) || hasText(iban);
	}

	// not null and not blank after trim
	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	// shared helper - search path uses this to build one CustomerDirectoryRow dto
	private CustomerDirectoryRow toDirectoryRow(UserRegistration customer) {
		List<BankAccount> userAccounts = bankAccountRepository.findAllByOwner_Id(customer.getId());

		// closed accounts hidden in search results - cant transfer to inactive iban
		List<CustomerAccountRow> activeAccountRows = userAccounts.stream()
				.filter(BankAccount::isActive)
				.map(CustomerAccountRow::fromBankAccount) // BankAccount entity -> dto
				.toList();

		String approvalStatus = null;
		if (customer.getCustomerApprovalStatus() != null) {
			approvalStatus = customer.getCustomerApprovalStatus().name(); // PENDING / APPROVED / DENIED
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

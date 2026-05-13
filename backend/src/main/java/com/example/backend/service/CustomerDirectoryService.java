package com.example.backend.service;

import com.example.backend.domain.BankAccount;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.UserRegistrationRepository;
import com.example.backend.web.dto.CustomerAccountRow;
import com.example.backend.web.dto.CustomerDirectoryRow;
import com.example.backend.web.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerDirectoryService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final BankAccountRepository bankAccountRepository;

	public CustomerDirectoryService(
		UserRegistrationRepository userRegistrationRepository,
		BankAccountRepository bankAccountRepository
	) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.bankAccountRepository = bankAccountRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<CustomerDirectoryRow> listCustomerDirectory(Pageable pageable, Boolean hasAccountFilter) {
		Page<UserRegistration> customerPage = Boolean.FALSE.equals(hasAccountFilter)
			? userRegistrationRepository.findCustomersWhoHaveNoBankAccounts(pageable)
			: userRegistrationRepository.findAllCustomers(pageable);

		List<Long> ownerIdsOnPage = customerPage.getContent().stream().map(UserRegistration::getId).toList();
		List<BankAccount> bankAccountsForOwnersOnPage = ownerIdsOnPage.isEmpty()
			? List.of()
			: bankAccountRepository.findByOwner_IdIn(ownerIdsOnPage);

		Map<Long, List<BankAccount>> bankAccountsGroupedByOwnerId = bankAccountsForOwnersOnPage.stream()
			.collect(Collectors.groupingBy(bankAccount -> bankAccount.getOwner().getId()));

		List<CustomerDirectoryRow> directoryRows = customerPage.getContent().stream()
			.map(customerRegistration -> toDirectoryRow(
				customerRegistration,
				bankAccountsGroupedByOwnerId.getOrDefault(customerRegistration.getId(), List.of())
			))
			.toList();

		return PageResponse.fromPage(customerPage, directoryRows);
	}

	private CustomerDirectoryRow toDirectoryRow(UserRegistration customerRegistration, List<BankAccount> bankAccounts) {
		List<CustomerAccountRow> accountRows = bankAccounts.stream()
			.map(bankAccount -> new CustomerAccountRow(
				bankAccount.getIban(),
				bankAccount.getAccountType().name(),
				bankAccount.isActive(),
				bankAccount.getBalance(),
				bankAccount.getMinimumAllowedBalance(),
				bankAccount.getDailyOutgoingTransferLimit()
			))
			.toList();

		CustomerApprovalStatus approvalStatus = customerRegistration.getCustomerApprovalStatus();
		String approvalStatusName = approvalStatus != null ? approvalStatus.name() : null;

		return new CustomerDirectoryRow(
			customerRegistration.getId(),
			customerRegistration.getFirstName(),
			customerRegistration.getLastName(),
			customerRegistration.getEmail(),
			approvalStatusName,
			accountRows
		);
	}
}

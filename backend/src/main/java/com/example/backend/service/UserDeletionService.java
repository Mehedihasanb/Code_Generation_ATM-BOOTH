package com.example.backend.service;

import com.example.backend.domain.BankAccount;
import com.example.backend.domain.UserRegistration;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.policy.UserDeletionPolicy;
import com.example.backend.repository.BankAccountRepository;
import com.example.backend.repository.TransactionRepository;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

//  delete service - soft/hard delete + employee reactivate, called from AuthController + UserController
@Service
public class UserDeletionService {

	private final UserRegistrationRepository userRegistrationRepository;
	private final BankAccountRepository bankAccountRepository;
	private final TransactionRepository transactionRepository;
	private final UserDeletionPolicy userDeletionPolicy;

	public UserDeletionService(
			UserRegistrationRepository userRegistrationRepository,
			BankAccountRepository bankAccountRepository,
			TransactionRepository transactionRepository,
			UserDeletionPolicy userDeletionPolicy) {
		this.userRegistrationRepository = userRegistrationRepository;
		this.bankAccountRepository = bankAccountRepository;
		this.transactionRepository = transactionRepository;
		this.userDeletionPolicy = userDeletionPolicy;
	}

	// shared entry - DELETE /auth/me (self) or DELETE /users/{id} (employee)
	@Transactional
	public void deleteAccount(String requesterEmail, Long targetUserId, boolean permanent) {
		// jwt email from Authentication.getName() in the controller
		UserRegistration requester = userRegistrationRepository.findByEmail(requesterEmail.trim().toLowerCase())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		userDeletionPolicy.requireNotDeleted(requester);

		UserRegistration target = resolveTarget(requester, targetUserId);
		userDeletionPolicy.requireEmployeeDeletingOther(requester, target);

		if (target.isDeleted() && !permanent) {
			throw new BadRequestException("Account is already deactivated");
		}

		if (permanent) {
			hardDelete(target);
		} else {
			softDelete(target);
		}
	}

	// POST /users/{id}/reactivate - undo soft delete
	@Transactional
	public void reactivateAccount(String requesterEmail, Long targetUserId) {
		UserRegistration requester = userRegistrationRepository.findByEmail(requesterEmail.trim().toLowerCase())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		userDeletionPolicy.requireNotDeleted(requester);
		userDeletionPolicy.requireEmployee(requester);

		UserRegistration target = userRegistrationRepository.findById(targetUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		userDeletionPolicy.requireDeletedForReactivation(target);

		// turn all their accounts back on (soft delete had set active=false on each)
		List<BankAccount> accounts = bankAccountRepository.findAllByOwner_Id(target.getId());
		for (BankAccount account : accounts) {
			account.setActive(true);
		}
		bankAccountRepository.saveAll(accounts);

		target.setDeleted(false);
		userRegistrationRepository.save(target);
	}

	// null targetUserId = delete yourself (DELETE /auth/me)
	private UserRegistration resolveTarget(UserRegistration requester, Long targetUserId) {
		if (targetUserId == null || targetUserId.equals(requester.getId())) {
			return requester;
		}
		return userRegistrationRepository.findById(targetUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	// soft delete - keep user row, block login, hide from directory
	private void softDelete(UserRegistration user) {
		List<BankAccount> accounts = bankAccountRepository.findAllByOwner_Id(user.getId());
		for (BankAccount account : accounts) {
			account.setActive(false); // same idea as AccountService.closeAccountByIban
		}
		bankAccountRepository.saveAll(accounts);

		user.setDeleted(true); // email still taken - register blocked till hard delete
		userRegistrationRepository.save(user);
	}

	// hard delete - remove row + accounts + transactions, email can register again
	private void hardDelete(UserRegistration user) {
		userDeletionPolicy.requireCanHardDeleteEmployee(user);

		List<BankAccount> accounts = bankAccountRepository.findAllByOwner_Id(user.getId());
		for (BankAccount account : accounts) {
			if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
				throw new BadRequestException(
						"Account balance must be zero before permanent deletion on IBAN " + account.getIban());
			}
		}

		// fk order: transactions point at accounts, so delete tx rows first
		transactionRepository.deleteAllForUser(user.getId());
		bankAccountRepository.deleteAll(accounts);
		userRegistrationRepository.delete(user);
	}
}

package com.example.backend.policy;

import com.example.backend.domain.UserRegistration;
import com.example.backend.exception.BadRequestException;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.stereotype.Component;

// rules for account deletion + reactivation - keeps UserDeletionService thin (SRP)
@Component
public class UserDeletionPolicy {

	private final UserRegistrationRepository userRegistrationRepository;

	public UserDeletionPolicy(UserRegistrationRepository userRegistrationRepository) {
		this.userRegistrationRepository = userRegistrationRepository;
	}

	public void requireNotDeleted(UserRegistration user) {
		if (user.isDeleted()) {
			// used on login (RegistrationService) and before delete/reactivate actions
			throw new BadRequestException("This account has been deactivated");
		}
	}

	// hard delete only - blocks wiping the last employee so we dont lock ourselves out
	public void requireCanHardDeleteEmployee(UserRegistration employee) {
		if (!"EMPLOYEE".equals(employee.getRole())) {
			return;
		}
		if (userRegistrationRepository.countByRoleAndDeletedFalse("EMPLOYEE") <= 1) {
			throw new BadRequestException("Cannot permanently delete the last active employee");
		}
	}

	// self-delete is fine for anyone; deleting another user's id needs employee role
	public void requireEmployeeDeletingOther(UserRegistration requester, UserRegistration target) {
		if (requester.getId().equals(target.getId())) {
			return; // DELETE /auth/me path
		}
		if (!"EMPLOYEE".equals(requester.getRole())) {
			throw new BadRequestException("Only employees can delete another user's account");
		}
	}

	// reactivate is employee-only - customers cant undo their own deactivation
	public void requireEmployee(UserRegistration requester) {
		if (!"EMPLOYEE".equals(requester.getRole())) {
			throw new BadRequestException("Only employees can perform this action");
		}
	}

	// can only reactivate someone who was soft-deleted, not already active
	public void requireDeletedForReactivation(UserRegistration user) {
		if (!user.isDeleted()) {
			throw new BadRequestException("Account is already active");
		}
	}
}

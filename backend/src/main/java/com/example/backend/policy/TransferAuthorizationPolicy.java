package com.example.backend.policy;

import com.example.backend.domain.BankAccount;
import com.example.backend.domain.UserRegistration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TransferAuthorizationPolicy {

	public void requireCanInitiateFromAccount(UserRegistration initiator, BankAccount fromAccount) {
		boolean ownsAccount = fromAccount.getOwner().getId().equals(initiator.getId());
		boolean isEmployee = "EMPLOYEE".equals(initiator.getRole());
		if (!ownsAccount && !isEmployee) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"You do not have permission to transfer from this account.");
		}
	}

	public void requireActiveAccounts(BankAccount fromAccount, BankAccount toAccount) {
		if (!fromAccount.isActive() || !toAccount.isActive()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or both accounts are inactive.");
		}
	}
}

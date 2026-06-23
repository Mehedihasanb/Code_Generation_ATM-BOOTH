import { authorizedFetch } from '@/composables/useAuthorizedFetch';

export type CheckingAccountOption = {
	iban: string;
	ownerName: string;
};

export type EmployeeTransferRequest = {
	fromIban: string;
	toIban: string;
	amount: number;
	description: string;
};

export async function fetchCheckingAccountOptions(): Promise<CheckingAccountOption[]> {
	const [usersResponse, accountsResponse] = await Promise.all([
		authorizedFetch('/users?size=500'),
		authorizedFetch('/accounts?type=CHECKING&size=500'),
	]);
	if (!usersResponse.ok || !accountsResponse.ok) {
		throw new Error('Failed to load account list.');
	}

	const usersData = await usersResponse.json();
	const accountsData = await accountsResponse.json();
	const nameByUserId = new Map<number, string>(
		(usersData.content || []).map((user: { id: number; firstName: string; lastName: string }) => [
			user.id,
			`${user.firstName} ${user.lastName}`,
		])
	);

	return (accountsData.content || []).map((acc: { iban: string; userId: number }) => ({
		iban: acc.iban,
		ownerName: nameByUserId.get(acc.userId) ?? 'Unknown',
	}));
}

export async function submitEmployeeTransfer(request: EmployeeTransferRequest): Promise<void> {
	const response = await authorizedFetch('/transactions', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({
			fromIban: request.fromIban,
			toIban: request.toIban,
			amount: request.amount,
			type: 'TRANSFER',
			description: request.description,
		}),
	});

	if (!response.ok) {
		let errorMessage = 'Transfer failed.';
		try {
			const errorData = await response.json();
			errorMessage = errorData.message || errorMessage;
		} catch {
			errorMessage = `Error: ${response.status} ${response.statusText}`;
		}
		throw new Error(errorMessage);
	}
}

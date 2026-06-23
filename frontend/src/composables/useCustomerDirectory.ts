import { authorizedFetch } from '@/composables/useAuthorizedFetch';

type PageResult<T> = {
	content: T[];
	number: number;
	totalPages: number;
	totalElements: number;
};

export type CustomerRow = {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	status?: string;
};

export type EmployeeAccountRow = {
	iban: string;
	minimumBalanceLimit?: number;
	dailyTransferLimit?: number;
};

export type CustomerTransactionRow = {
	id: number;
	transactionId: number;
	type: string;
	fromIban?: string;
	toIban?: string;
	amount: number;
	timestamp: string;
};

export type CustomerListResult = {
	customers: CustomerRow[];
	page: number;
	totalPages: number;
	totalElements: number;
};

export type CustomerDetailResult = {
	user: CustomerRow;
	accounts: EmployeeAccountRow[];
	transactions: CustomerTransactionRow[];
	page: number;
	totalPages: number;
};

export type AccountLimitsUpdate = {
	minimumBalanceLimit: number;
	dailyTransferLimit: number;
};

async function readPage<T>(response: Response, fallbackMessage: string): Promise<PageResult<T>> {
	if (!response.ok) {
		throw new Error(fallbackMessage);
	}
	return (await response.json()) as PageResult<T>;
}

export async function fetchActiveCustomers(pageIndex: number): Promise<CustomerListResult> {
	const params = new URLSearchParams({
		page: String(pageIndex),
		size: '10',
		status: 'ACTIVE',
	});
	const data = await readPage<CustomerRow>(
		await authorizedFetch(`/users?${params.toString()}`),
		'Could not load customers.'
	);
	return {
		customers: data.content ?? [],
		page: data.number ?? pageIndex,
		totalPages: data.totalPages ?? 0,
		totalElements: data.totalElements ?? data.content?.length ?? 0,
	};
}

export async function searchActiveCustomers(query: string): Promise<CustomerListResult> {
	const params = new URLSearchParams({ search: query, size: '50', status: 'ACTIVE' });
	const data = await readPage<CustomerRow>(
		await authorizedFetch(`/users?${params.toString()}`),
		'Search failed.'
	);
	const customers = data.content ?? [];
	return {
		customers,
		page: 0,
		totalPages: 1,
		totalElements: customers.length,
	};
}

export async function fetchEmployeeAccounts(userId: number): Promise<EmployeeAccountRow[]> {
	const response = await authorizedFetch(`/accounts?userId=${userId}&size=10`);
	const data = await readPage<EmployeeAccountRow>(response, 'Could not load account limits.');
	return data.content ?? [];
}

export async function fetchCustomerDetail(userId: number): Promise<CustomerRow> {
	const response = await authorizedFetch(`/users/${userId}`);
	if (!response.ok) {
		throw new Error('Failed to load customer detail.');
	}
	return (await response.json()) as CustomerRow;
}

export async function fetchCustomerTransactions(
	userId: number,
	pageIndex: number
): Promise<{ transactions: CustomerTransactionRow[]; page: number; totalPages: number }> {
	const response = await authorizedFetch(
		`/transactions?customerId=${userId}&page=${pageIndex}&size=10`
	);
	const data = await readPage<CustomerTransactionRow & { id: number }>(
		response,
		'Failed to load user transactions.'
	);
	return {
		transactions: (data.content ?? []).map((tx) => ({
			...tx,
			transactionId: tx.id,
		})),
		page: data.number,
		totalPages: data.totalPages,
	};
}

export async function loadCustomerDetailPage(
	userId: number,
	pageIndex: number
): Promise<CustomerDetailResult> {
	const [user, accounts, txPage] = await Promise.all([
		fetchCustomerDetail(userId),
		fetchEmployeeAccounts(userId),
		fetchCustomerTransactions(userId, pageIndex),
	]);
	return {
		user,
		accounts,
		transactions: txPage.transactions,
		page: txPage.page,
		totalPages: txPage.totalPages,
	};
}

export async function updateAccountLimits(iban: string, limits: AccountLimitsUpdate): Promise<void> {
	const response = await authorizedFetch(`/accounts/${iban}`, {
		method: 'PATCH',
		body: JSON.stringify(limits),
	});
	if (!response.ok) {
		const message = await response.text();
		throw new Error(message || `Update failed (${response.status})`);
	}
}

export async function updateAllAccountLimits(
	accounts: EmployeeAccountRow[],
	limits: AccountLimitsUpdate
): Promise<void> {
	for (const account of accounts) {
		await updateAccountLimits(account.iban, limits);
	}
}

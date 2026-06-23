import { authorizedFetch } from '@/composables/useAuthorizedFetch';

export type SystemTransactionRow = {
	id: number;
	transactionId: number;
	type: string;
	fromIban?: string;
	toIban?: string;
	amount: number;
	timestamp: string;
	initiatingUserEmail?: string;
	initiatingUser?: string;
};

export type TransactionFilters = {
	startDate: string;
	endDate: string;
	amount: string;
	amountOperator: string;
};

export type TransactionPage = {
	transactions: SystemTransactionRow[];
	page: number;
	totalPages: number;
};

export async function fetchSystemTransactions(
	pageIndex: number,
	filters: TransactionFilters
): Promise<TransactionPage> {
	let url = `/transactions?page=${pageIndex}&size=10&sort=timestamp,desc`;

	if (filters.startDate) url += `&startDate=${filters.startDate}`;
	if (filters.endDate) url += `&endDate=${filters.endDate}`;
	if (filters.amount) {
		url += `&amount=${filters.amount}&amountOperator=${filters.amountOperator}`;
	}

	const response = await authorizedFetch(url);

	if (!response.ok) {
		let errorMessage = 'Failed to load system transactions.';
		try {
			const errorData = await response.json();
			errorMessage = errorData.message || errorMessage;
		} catch {
			errorMessage = `Error: ${response.status} ${response.statusText}`;
		}
		throw new Error(errorMessage);
	}

	const pageData = await response.json();
	return {
		transactions: (pageData.content || []).map((tx: SystemTransactionRow) => ({
			...tx,
			transactionId: tx.id,
			initiatingUser: tx.initiatingUserEmail,
		})),
		page: pageData.number,
		totalPages: pageData.totalPages,
	};
}

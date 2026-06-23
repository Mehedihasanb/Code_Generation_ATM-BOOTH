import { authorizedFetch } from '@/composables/useAuthorizedFetch';

export type CustomerTransactionRow = {
	id: number;
	transactionId: number;
	description?: string;
	amount: number;
	timestamp: string;
	fromIban: string;
	toIban: string;
	type: 'INCOMING' | 'OUTGOING';
	counterpartIban: string;
};

export type CustomerTransactionFilters = {
	startDate: string;
	endDate: string;
	amount: string;
	amountOperator: string;
	counterpartIban: string;
};

export type CustomerTransactionPage = {
	transactions: CustomerTransactionRow[];
	page: number;
	totalPages: number;
};

export async function fetchCustomerTransactions(
	accountIban: string,
	pageIndex: number,
	filters: CustomerTransactionFilters
): Promise<CustomerTransactionPage> {
	let url = `/transactions?accountIban=${accountIban}&page=${pageIndex}&size=10`;

	if (filters.startDate) url += `&startDate=${filters.startDate}`;
	if (filters.endDate) url += `&endDate=${filters.endDate}`;
	if (filters.amount) {
		url += `&amount=${filters.amount}&amountOperator=${filters.amountOperator}`;
	}
	if (filters.counterpartIban) {
		const compactIban = filters.counterpartIban.replace(/\s/g, '').toUpperCase();
		url += `&counterpartIban=${encodeURIComponent(compactIban)}`;
	}

	const response = await authorizedFetch(url);
	if (!response.ok) {
		throw new Error('Failed to load transactions.');
	}

	const pageData = await response.json();
	return {
		transactions: (pageData.content || []).map((tx: {
			id: number;
			description?: string;
			amount: number;
			timestamp: string;
			fromIban: string;
			toIban: string;
		}) => ({
			...tx,
			transactionId: tx.id,
			type: tx.toIban === accountIban ? 'INCOMING' : 'OUTGOING',
			counterpartIban: tx.toIban === accountIban ? tx.fromIban : tx.toIban,
		})),
		page: pageData.number,
		totalPages: pageData.totalPages,
	};
}

import { authorizedFetch } from '@/composables/useAuthorizedFetch';

export type TransferTarget = {
	iban: string;
	firstName: string;
	lastName: string;
};

export type CustomerTransferRequest = {
	fromIban: string;
	toIban: string;
	amount: number | '';
	description: string;
};

export function looksLikeIbanQuery(query: string): boolean {
	const compact = query.replace(/\s/g, '').toUpperCase();
	if (compact.length < 2 || !/^[A-Z0-9]+$/.test(compact)) {
		return false;
	}
	return /^[A-Z]{2}$/.test(compact) || compact.startsWith('NL') || /\d/.test(compact);
}

export function normalizeSearchQuery(value: string): string {
	const collapsed = value.replace(/\s+/g, ' ').trim();
	if (looksLikeIbanQuery(collapsed)) {
		return collapsed.replace(/\s/g, '').toUpperCase();
	}
	return collapsed;
}

export function sortRecipientsForQuery(query: string, results: TransferTarget[]): TransferTarget[] {
	if (!looksLikeIbanQuery(query)) {
		return results;
	}
	const needle = query.toLowerCase();
	return [...results].sort((a, b) => {
		const aIban = a.iban.toLowerCase();
		const bIban = b.iban.toLowerCase();
		const aStarts = aIban.startsWith(needle) ? 0 : 1;
		const bStarts = bIban.startsWith(needle) ? 0 : 1;
		if (aStarts !== bStarts) {
			return aStarts - bStarts;
		}
		return aIban.localeCompare(bIban);
	});
}

async function fetchTransferTargetPage(name: string, size: number): Promise<TransferTarget[]> {
	const response = await authorizedFetch(
		`/accounts/transfer-targets?name=${encodeURIComponent(name)}&size=${size}`
	);
	if (!response.ok) {
		throw new Error('Failed to search transfer targets.');
	}
	const data = await response.json();
	return data.content || [];
}

export async function searchTransferTargets(query: string): Promise<TransferTarget[]> {
	const pageSize = looksLikeIbanQuery(query) ? 50 : 20;
	let results = await fetchTransferTargetPage(query, pageSize);

	if (results.length === 0 && query.includes(' ')) {
		const parts = query.split(/\s+/).filter(Boolean);
		const firstNameQuery = parts[0];
		const remainder = parts.slice(1).join(' ').toLowerCase();
		const fallbackResults = await fetchTransferTargetPage(firstNameQuery, 20);
		results = fallbackResults.filter((target) => {
			const fullName = `${target.firstName} ${target.lastName}`.toLowerCase();
			return (
				fullName.includes(remainder) ||
				target.lastName.toLowerCase().includes(remainder) ||
				target.iban.toLowerCase().includes(remainder)
			);
		});
	}

	return sortRecipientsForQuery(query, results);
}

export async function submitCustomerTransfer(request: CustomerTransferRequest): Promise<void> {
	const response = await authorizedFetch('/transactions', {
		method: 'POST',
		body: JSON.stringify({
			fromIban: request.fromIban,
			toIban: request.toIban,
			amount: request.amount,
			type: 'TRANSFER',
			description: request.description || 'Transfer',
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

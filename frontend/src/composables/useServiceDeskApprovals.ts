import { authorizedFetch } from '@/composables/useAuthorizedFetch';

export type PendingCustomer = {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	bsn?: string;
	bsnNumber?: string;
};

export async function fetchPendingCustomers(): Promise<PendingCustomer[]> {
	const response = await authorizedFetch('/users?status=PENDING');
	if (!response.ok) {
		throw new Error('Failed to fetch pending customers.');
	}
	const pageData = await response.json();
	return pageData.content || pageData.items || pageData;
}

export async function approveCustomer(id: number): Promise<void> {
	const response = await authorizedFetch(`/users/${id}`, {
		method: 'PATCH',
		body: JSON.stringify({ status: 'ACTIVE' }),
	});
	if (!response.ok) {
		const errorText = await response.text();
		throw new Error(errorText || `Approval failed (${response.status})`);
	}
}

export async function denyCustomer(id: number): Promise<void> {
	const response = await authorizedFetch(`/users/${id}`, {
		method: 'PATCH',
		body: JSON.stringify({ status: 'CLOSED' }),
	});
	if (!response.ok) {
		throw new Error('Denial failed.');
	}
}

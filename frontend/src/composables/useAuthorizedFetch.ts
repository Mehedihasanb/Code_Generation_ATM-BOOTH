import { apiUrl } from '@/config/api';

const tokenStorageKey = 'code-generation-token';

export async function authorizedFetch(input: string, init: RequestInit = {}): Promise<Response> {
	const token = sessionStorage.getItem(tokenStorageKey);
	if (!token) {
		throw new Error('No authorization token found. Please log in again.');
	}

	const headers = new Headers(init.headers);
	headers.set('Authorization', `Bearer ${token}`);
	if (!headers.has('Content-Type') && init.body) {
		headers.set('Content-Type', 'application/json');
	}

	return fetch(apiUrl(input), { ...init, headers });
}

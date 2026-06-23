import { authorizedFetch } from '@/composables/useAuthorizedFetch';
import { parseApiErrorMessage } from '@/utils/apiError';

export type AtmTransactionResult = {
	amount: number;
};

export async function submitAtmWithdrawal(fromIban: string, amount: number): Promise<AtmTransactionResult> {
	const response = await authorizedFetch('/transactions', {
		method: 'POST',
		body: JSON.stringify({
			amount,
			fromIban,
			type: 'WITHDRAWAL',
		}),
	});

	if (!response.ok) {
		const message = await response.text();
		throw new Error(parseApiErrorMessage(message, `Withdrawal failed (${response.status})`));
	}

	return (await response.json()) as AtmTransactionResult;
}

export async function submitAtmDeposit(toIban: string, amount: number): Promise<AtmTransactionResult> {
	const response = await authorizedFetch('/transactions', {
		method: 'POST',
		body: JSON.stringify({
			amount,
			toIban,
			type: 'DEPOSIT',
		}),
	});

	if (!response.ok) {
		const message = await response.text();
		throw new Error(parseApiErrorMessage(message, `Deposit failed (${response.status})`));
	}

	return (await response.json()) as AtmTransactionResult;
}

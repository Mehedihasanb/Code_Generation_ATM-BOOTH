import { authorizedFetch } from '@/composables/useAuthorizedFetch';

export type OwnAccountRow = {
	iban: string;
	accountType: string;
	balance: number;
	minimumBalanceLimit: number;
	active: boolean;
};

export type MyAccountsSummary = {
	accounts: OwnAccountRow[];
	combinedBalance: number;
};

type OwnAccountResponse = {
	iban: string;
	type: string;
	balance: number;
	minimumBalanceLimit: number;
	dailyTransferLimit: number;
	status: 'ACTIVE' | 'CLOSED';
};

type OwnAccountsPage = {
	content: OwnAccountResponse[];
};

export async function fetchMyAccounts(): Promise<MyAccountsSummary> {
	const response = await authorizedFetch('/accounts/me?size=100');
	if (!response.ok) {
		const errorText = await response.text();
		throw new Error(errorText || `Failed to load accounts (${response.status})`);
	}

	const page = (await response.json()) as OwnAccountsPage;
	const accounts = (page.content ?? []).map((account) => ({
		iban: account.iban,
		accountType: account.type,
		balance: account.balance,
		minimumBalanceLimit: account.minimumBalanceLimit ?? 0,
		active: account.status === 'ACTIVE',
	}));
	const combinedBalance = accounts.reduce((sum, account) => sum + account.balance, 0);
	return { accounts, combinedBalance };
}

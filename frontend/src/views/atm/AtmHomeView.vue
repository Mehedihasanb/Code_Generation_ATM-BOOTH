<script setup lang="ts">
// ATM home — shows balances and links to withdraw and deposit.
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { fetchMyAccounts } from '@/composables/useMyAccounts';

type AccountSummary = {
	customerName: string;
	combinedBalance: number;
	accounts: { iban: string; accountType: string; balance: number; minimumBalanceLimit: number }[];
};

const auth = useAuthStore();
const router = useRouter();

const summary = ref<AccountSummary | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

async function loadAccounts() {
	loading.value = true;
	error.value = null;
	try {
		const result = await fetchMyAccounts();
		summary.value = {
			customerName: auth.firstName ?? '',
			combinedBalance: result.combinedBalance,
			accounts: result.accounts.map((account) => ({
				iban: account.iban,
				accountType: account.accountType,
				balance: account.balance,
				minimumBalanceLimit: account.minimumBalanceLimit,
			})),
		};
	} catch (err) {
		error.value = err instanceof Error ? err.message : String(err);
	} finally {
		loading.value = false;
	}
}

function logout() {
	auth.logout();
	router.push('/atm/login');
}

function formatCurrency(amount: number) {
	return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(amount);
}

onMounted(() => {
	loadAccounts();
});
</script>

<template>
	<div class="atm-shell">
		<section class="atm-panel atm-home-panel">
			<header class="atm-home-header">
				<div>
					<p class="atm-session-label">ATM session active</p>
					<h1>Welcome, {{ auth.firstName || 'Customer' }}</h1>
				</div>
				<button type="button" class="atm-btn atm-btn-secondary" @click="logout">End session</button>
			</header>

			<p v-if="loading" class="muted">Loading your accounts...</p>
			<p v-else-if="error" class="error">{{ error }}</p>

			<template v-else-if="summary">
				<div class="atm-balance-card">
					<span class="atm-balance-label">Total balance</span>
					<strong class="atm-balance-value">{{ formatCurrency(summary.combinedBalance) }}</strong>
					<span class="muted">{{ summary.customerName }}</span>
				</div>

				<ul class="atm-account-list">
					<li v-for="account in summary.accounts" :key="account.iban">
						<span class="atm-account-type">{{ account.accountType }}</span>
						<span class="atm-account-iban">{{ account.iban }}</span>
						<span class="atm-account-balance">{{ formatCurrency(account.balance) }}</span>
					</li>
				</ul>
			</template>

			<nav class="atm-menu" aria-label="ATM services">
				<button type="button" class="atm-menu-btn" @click="router.push('/atm/withdraw')">
					<span class="atm-menu-label">OUT</span>
					Withdraw
				</button>
				<button type="button" class="atm-menu-btn" @click="router.push('/atm/deposit')">
					<span class="atm-menu-label">IN</span>
					Deposit
				</button>
				<button type="button" class="atm-menu-btn" @click="loadAccounts">
					<span class="atm-menu-label">REF</span>
					Refresh
				</button>
			</nav>

			<p class="atm-footnote muted">Choose a service above.</p>
		</section>
	</div>
</template>

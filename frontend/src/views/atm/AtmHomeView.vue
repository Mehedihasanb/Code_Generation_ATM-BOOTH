<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';

type AccountSummary = {
	customerName: string;
	combinedBalance: number;
	accounts: { iban: string; accountType: string; balance: number; absoluteLimit: number }[];
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
		const response = await authorizedFetch('/accounts/mine');
		if (!response.ok) {
			throw new Error('Could not load your accounts.');
		}
		summary.value = await response.json();
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
					<span>💵</span>
					Withdraw
				</button>
				<button type="button" class="atm-menu-btn" @click="router.push('/atm/deposit')">
					<span>📥</span>
					Deposit
				</button>
				<button type="button" class="atm-menu-btn" @click="loadAccounts">
					<span>🔄</span>
					Refresh balance
				</button>
			</nav>

			<p class="atm-footnote muted">Choose a service above.</p>
		</section>
	</div>
</template>

<style scoped>
.atm-shell {
	min-height: calc(100vh - 2rem);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 1.5rem;
	background: linear-gradient(160deg, #0f172a 0%, #1e3a5f 45%, #0f172a 100%);
}

.atm-home-panel {
	width: 100%;
	max-width: 520px;
	background: #1e293b;
	border: 2px solid #334155;
	border-radius: 16px;
	padding: 1.75rem;
	color: #f8fafc;
	box-shadow: 0 24px 48px rgba(0, 0, 0, 0.35);
}

.atm-home-header {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
	gap: 1rem;
	margin-bottom: 1.5rem;
}

.atm-session-label {
	margin: 0 0 0.25rem;
	font-size: 0.75rem;
	text-transform: uppercase;
	letter-spacing: 0.08em;
	color: #4ade80;
	font-weight: 700;
}

.atm-home-header h1 {
	margin: 0;
	font-size: 1.35rem;
	color: #f8fafc;
}

.atm-btn {
	padding: 0.5rem 1rem;
	border-radius: 8px;
	border: none;
	font: inherit;
	font-weight: 600;
	cursor: pointer;
	white-space: nowrap;
}

.atm-btn-secondary {
	background: #334155;
	color: #e2e8f0;
}

.atm-btn-secondary:hover {
	background: #475569;
}

.atm-balance-card {
	background: #0f172a;
	border: 1px solid #334155;
	border-radius: 12px;
	padding: 1.25rem;
	margin-bottom: 1.25rem;
	display: flex;
	flex-direction: column;
	gap: 0.35rem;
}

.atm-balance-label {
	font-size: 0.85rem;
	color: #94a3b8;
}

.atm-balance-value {
	font-size: 2rem;
	color: #4ade80;
}

.atm-account-list {
	list-style: none;
	margin: 0 0 1.5rem;
	padding: 0;
	display: grid;
	gap: 0.5rem;
}

.atm-account-list li {
	display: grid;
	grid-template-columns: auto 1fr auto;
	gap: 0.5rem 1rem;
	align-items: center;
	padding: 0.65rem 0.85rem;
	background: #0f172a;
	border-radius: 8px;
	font-size: 0.9rem;
}

.atm-account-type {
	font-weight: 700;
	color: #38bdf8;
}

.atm-account-iban {
	font-family: ui-monospace, monospace;
	font-size: 0.8rem;
	color: #94a3b8;
}

.atm-account-balance {
	font-weight: 600;
}

.atm-menu {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 0.75rem;
}

.atm-menu-btn {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 0.35rem;
	padding: 1rem 0.5rem;
	border-radius: 10px;
	border: 1px solid #475569;
	background: #0f172a;
	color: #e2e8f0;
	font: inherit;
	font-size: 0.85rem;
	font-weight: 600;
	cursor: pointer;
}

.atm-menu-btn span:first-child {
	font-size: 1.5rem;
}

.atm-menu-btn:not(:disabled):hover {
	border-color: #38bdf8;
	background: #1e293b;
}

.atm-menu-btn:disabled {
	opacity: 0.45;
	cursor: not-allowed;
}

.atm-footnote {
	margin: 1rem 0 0;
	text-align: center;
	font-size: 0.8rem;
	color: #64748b;
}
</style>

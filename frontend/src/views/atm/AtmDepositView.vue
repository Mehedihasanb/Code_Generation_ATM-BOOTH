<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';

type AccountSummary = {
	accounts: { iban: string; accountType: string; balance: number; absoluteLimit?: number }[];
};

const router = useRouter();

const summary = ref<AccountSummary | null>(null);
const selectedIban = ref('');
const amount = ref<number | null>(null);
const loading = ref(true);
const submitting = ref(false);
const error = ref<string | null>(null);
const success = ref<string | null>(null);

const checkingAccounts = computed(() =>
	(summary.value?.accounts ?? []).filter((account) => account.accountType === 'CHECKING')
);

async function loadAccounts() {
	loading.value = true;
	error.value = null;
	try {
		const response = await authorizedFetch('/accounts/mine');
		if (!response.ok) {
			throw new Error('Could not load accounts.');
		}
		summary.value = await response.json();
		const accounts = checkingAccounts.value;
		if (accounts.length === 1) {
			selectedIban.value = accounts[0].iban;
		}
	} catch (err) {
		error.value = err instanceof Error ? err.message : String(err);
	} finally {
		loading.value = false;
	}
}

async function submitDeposit() {
	if (!amount.value || amount.value <= 0) {
		error.value = 'Enter an amount greater than zero.';
		return;
	}
	if (!selectedIban.value) {
		error.value = 'Select a checking account.';
		return;
	}

	submitting.value = true;
	error.value = null;
	success.value = null;

	try {
		const body: { amount: number; toIban: string } = {
			amount: Number(amount.value),
			toIban: selectedIban.value,
		};
		const response = await authorizedFetch('/atm/deposit', {
			method: 'POST',
			body: JSON.stringify(body),
		});

		if (!response.ok) {
			const message = await response.text();
			throw new Error(message || `Deposit failed (${response.status})`);
		}

		const result = await response.json();
		success.value = `Deposit successful. Accepted €${Number(result.amount).toFixed(2)}. New balance: €${Number(result.newBalance).toFixed(2)}.`;
		amount.value = null;
		await loadAccounts();
	} catch (err) {
		error.value = err instanceof Error ? err.message : String(err);
	} finally {
		submitting.value = false;
	}
}

function formatCurrency(value: number) {
	return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(value);
}

function roomLeft(account: { balance: number; absoluteLimit?: number }) {
	const cap = account.absoluteLimit ?? 0;
	return Math.max(0, cap - account.balance);
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
					<p class="atm-session-label">Deposit cash</p>
					<h1>How much are you depositing?</h1>
				</div>
				<button type="button" class="atm-btn atm-btn-secondary" @click="router.push('/atm/home')">
					&larr; Back
				</button>
			</header>

			<p v-if="loading" class="muted">Loading accounts...</p>

			<form v-else class="atm-form" @submit.prevent="submitDeposit">
				<label v-if="checkingAccounts.length > 1">
					<span>Checking account</span>
					<select v-model="selectedIban" required>
						<option disabled value="">Select account</option>
						<option v-for="account in checkingAccounts" :key="account.iban" :value="account.iban">
							{{ account.iban }} — {{ formatCurrency(account.balance) }}
							(room: {{ formatCurrency(roomLeft(account)) }})
						</option>
					</select>
				</label>

				<p v-else-if="checkingAccounts.length === 1" class="muted account-hint">
					To: {{ checkingAccounts[0].iban }} ({{ formatCurrency(checkingAccounts[0].balance) }},
					room: {{ formatCurrency(roomLeft(checkingAccounts[0])) }})
				</p>
				<p v-else class="error">No checking account available.</p>

				<label>
					<span>Amount (€)</span>
					<input
						v-model.number="amount"
						type="number"
						min="0.01"
						step="0.01"
						required
						placeholder="0.00" />
				</label>

				<p v-if="error" class="error">{{ error }}</p>
				<p v-if="success" class="success-msg">{{ success }}</p>

				<button
					type="submit"
					class="atm-btn atm-btn-primary"
					:disabled="submitting || checkingAccounts.length === 0">
					{{ submitting ? 'Processing...' : 'Deposit' }}
				</button>
			</form>
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
	max-width: 420px;
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
	margin-bottom: 1.25rem;
}

.atm-session-label {
	margin: 0 0 0.25rem;
	font-size: 0.75rem;
	text-transform: uppercase;
	letter-spacing: 0.08em;
	color: #38bdf8;
	font-weight: 700;
}

.atm-home-header h1 {
	margin: 0;
	font-size: 1.25rem;
	color: #f8fafc;
}

.atm-form {
	display: grid;
	gap: 1rem;
}

.atm-form label {
	display: grid;
	gap: 0.35rem;
	font-weight: 600;
	font-size: 0.9rem;
	color: #cbd5e1;
}

.atm-form input,
.atm-form select {
	padding: 0.75rem;
	border: 1px solid #475569;
	border-radius: 8px;
	background: #0f172a;
	color: #f8fafc;
	font: inherit;
}

.atm-btn {
	padding: 0.85rem 1.25rem;
	border-radius: 10px;
	border: none;
	font: inherit;
	font-weight: 700;
	cursor: pointer;
}

.atm-btn-secondary {
	background: #334155;
	color: #e2e8f0;
}

.atm-btn-primary {
	background: linear-gradient(180deg, #38bdf8 0%, #0284c7 100%);
	color: #0c4a6e;
	width: 100%;
}

.atm-btn-primary:disabled {
	opacity: 0.6;
	cursor: not-allowed;
}

.account-hint {
	margin: 0;
	font-size: 0.9rem;
}

.success-msg {
	margin: 0;
	color: #38bdf8;
	font-weight: 600;
}
</style>

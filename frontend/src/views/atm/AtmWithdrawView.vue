<script setup lang="ts">
// ATM withdraw — submits POST /transactions; rules enforced by the backend.
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchMyAccounts } from '@/composables/useMyAccounts';
import { submitAtmWithdrawal } from '@/composables/useAtmTransactions';
type AccountSummary = {
	customerName: string;
	combinedBalance: number;
	accounts: { iban: string; accountType: string; balance: number }[];
};

const router = useRouter();

const summary = ref<AccountSummary | null>(null);
const selectedIban = ref('');
const amount = ref<number | null>(null);
const loading = ref(true);
const submitting = ref(false);
const error = ref<string | null>(null);
const success = ref<string | null>(null);

const accounts = computed(() => summary.value?.accounts ?? []);

const selectedAccount = computed(() =>
	accounts.value.find((account) => account.iban === selectedIban.value) ?? null
);
async function loadAccounts() {
	loading.value = true;
	error.value = null;
	try {
		const result = await fetchMyAccounts();
		summary.value = {
			customerName: '',
			combinedBalance: result.combinedBalance,
			accounts: result.accounts.map((account) => ({
				iban: account.iban,
				accountType: account.accountType,
				balance: account.balance,
			})),
		};
		if (accounts.value.length > 0 && !accounts.value.some((a) => a.iban === selectedIban.value)) {
			selectedIban.value = accounts.value[0].iban;
		}	} catch (err) {
		error.value = err instanceof Error ? err.message : String(err);
	} finally {
		loading.value = false;
	}
}

async function submitWithdraw() {
	submitting.value = true;
	error.value = null;
	success.value = null;

	try {
		const result = await submitAtmWithdrawal(selectedIban.value, Number(amount.value));
		success.value = `Withdrawal successful. Dispensed €${Number(result.amount).toFixed(2)}.`;
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

onMounted(() => {
	loadAccounts();
});
</script>

<template>
	<div class="atm-shell">
		<section class="atm-panel atm-home-panel">
			<header class="atm-home-header">
				<div>
					<p class="atm-session-label">Withdraw cash</p>
					<h1>How much would you like?</h1>
				</div>
				<button type="button" class="atm-btn atm-btn-secondary" @click="router.push('/atm/home')">
					&larr; Back
				</button>
			</header>

			<p v-if="loading" class="muted">Loading accounts...</p>

			<div v-else-if="selectedAccount" class="atm-balance-card">
				<span class="atm-balance-label">Available to withdraw</span>
				<strong class="atm-balance-value">{{ formatCurrency(selectedAccount.balance) }}</strong>
				<span class="muted atm-balance-iban">{{ selectedAccount.iban }}</span>
				<span v-if="summary" class="muted atm-balance-total">
					Total across accounts: {{ formatCurrency(summary.combinedBalance) }}
				</span>
			</div>

			<form v-if="!loading" class="atm-form" @submit.prevent="submitWithdraw">
				<label v-if="accounts.length > 1">
					<span>Account</span>
					<select v-model="selectedIban">
						<option disabled value="">Select account</option>
						<option v-for="account in accounts" :key="account.iban" :value="account.iban">
							{{ account.accountType }} — {{ account.iban }} — {{ formatCurrency(account.balance) }}
						</option>
					</select>
				</label>

				<p v-if="accounts.length === 0" class="error">No accounts available.</p>

				<label>
					<span>Amount (€)</span>
					<input
						v-model.number="amount"
						type="number"
						step="0.01"
						placeholder="0.00" />
				</label>

				<p v-if="error" class="error">{{ error }}</p>
				<p v-if="success" class="success-msg">{{ success }}</p>

				<button
					type="submit"
					class="atm-btn atm-btn-primary"
					:disabled="submitting || accounts.length === 0">					{{ submitting ? 'Processing...' : 'Withdraw' }}
				</button>
			</form>
		</section>
	</div>
</template>

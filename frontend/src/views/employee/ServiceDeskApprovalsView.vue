<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

type PendingCustomer = {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	customerApprovalStatus: string | null;
};

const pendingCustomers = ref<PendingCustomer[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

const selectedCustomer = ref<PendingCustomer | null>(null);
const showModal = ref(false);
const dailyLimit = ref(1000);
const minimumBalance = ref(0);

async function loadPendingCustomers() {
	loading.value = true;
	error.value = null;

	try {
		if (!auth.token) {
			error.value = 'Please log in as an employee first.';
			return;
		}

		const response = await fetch('/users?hasAccount=false&size=50', {
			headers: { Authorization: `Bearer ${auth.token}` },
		});

		if (response.status === 401) {
			error.value = 'Session expired. Please log in again.';
			return;
		}
		if (!response.ok) {
			throw new Error(`Failed to load pending customers (${response.status})`);
		}

		const page = await response.json();
		pendingCustomers.value = page.content ?? [];
	} catch (err) {
		error.value = err instanceof Error ? err.message : String(err);
	} finally {
		loading.value = false;
	}
}

function openReviewModal(customer: PendingCustomer) {
	selectedCustomer.value = customer;
	dailyLimit.value = 1000;
	minimumBalance.value = 0;
	showModal.value = true;
}

function closeReviewModal() {
	selectedCustomer.value = null;
	showModal.value = false;
}

async function submitApproval() {
	if (!selectedCustomer.value || !auth.token) {
		return;
	}

	try {
		const response = await fetch('/accounts', {
			method: 'POST',
			headers: {
				Authorization: `Bearer ${auth.token}`,
				'Content-Type': 'application/json',
			},
			body: JSON.stringify({
				customerRegistrationId: selectedCustomer.value.id,
				dailyOutgoingTransferLimit: dailyLimit.value,
				minimumAllowedBalance: minimumBalance.value,
			}),
		});

		if (!response.ok) {
			throw new Error('Approval failed');
		}

		alert(`Customer ${selectedCustomer.value.firstName} approved.`);
		closeReviewModal();
		await loadPendingCustomers();
	} catch {
		alert('Failed to approve customer.');
	}
}

async function submitDeny(id: number) {
	if (!confirm('Deny this application?')) {
		return;
	}
	if (!auth.token) {
		return;
	}

	try {
		const response = await fetch(`/auth/customers/${id}/deny`, {
			method: 'POST',
			headers: { Authorization: `Bearer ${auth.token}` },
		});

		if (!response.ok) {
			throw new Error('Denial failed');
		}

		alert('Customer denied.');
		await loadPendingCustomers();
	} catch {
		alert('Failed to deny customer.');
	}
}

onMounted(() => {
	loadPendingCustomers();
});
</script>

<template>
	<main class="dashboard-wrapper">
		<header class="dashboard-header">
			<div>
				<h1>Pending Approvals</h1>
				<p class="muted">Review new customer registrations.</p>
			</div>
			<button class="btn secondary-btn" @click="router.push('/service-desk')">Back to Dashboard</button>
		</header>

		<section class="panel content-section">
			<p v-if="loading">Loading applications...</p>
			<p v-else-if="error" class="error">{{ error }}</p>
			<p v-else-if="pendingCustomers.length === 0" class="muted">No pending registrations at this time.</p>

			<table v-else class="data-table">
				<thead>
					<tr>
						<th>Name</th>
						<th>Email</th>
						<th>Status</th>
						<th>Actions</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="customer in pendingCustomers" :key="customer.id">
						<td>{{ customer.firstName }} {{ customer.lastName }}</td>
						<td>{{ customer.email }}</td>
						<td>{{ customer.customerApprovalStatus ?? 'PENDING' }}</td>
						<td class="action-cell">
							<button class="btn primary-btn" @click="openReviewModal(customer)">Review</button>
							<button class="btn danger-btn" @click="submitDeny(customer.id)">Deny</button>
						</td>
					</tr>
				</tbody>
			</table>
		</section>

		<div v-if="showModal" class="modal-overlay">
			<div class="panel modal-content">
				<h2>Approve Customer</h2>
				<p class="muted">
					Approving <strong>{{ selectedCustomer?.firstName }} {{ selectedCustomer?.lastName }}</strong>
					creates checking and savings accounts.
				</p>

				<form class="auth-form" @submit.prevent="submitApproval">
					<label>
						<span>Daily transfer limit (€)</span>
						<input type="number" v-model="dailyLimit" required min="1" />
					</label>

					<label>
						<span>Minimum allowed balance (€)</span>
						<input type="number" v-model="minimumBalance" required max="0" />
						<small class="muted">Zero or negative (e.g. -500 for overdraft).</small>
					</label>

					<div class="button-group" style="margin-top: 20px">
						<button type="button" class="btn secondary-btn" @click="closeReviewModal">Cancel</button>
						<button type="submit" class="btn primary-btn">Approve</button>
					</div>
				</form>
			</div>
		</div>
	</main>
</template>

<style scoped>
.data-table {
	width: 100%;
	border-collapse: collapse;
	margin-top: 1rem;
}
.data-table th,
.data-table td {
	padding: 12px;
	text-align: left;
	border-bottom: 1px solid var(--border-color, #ccc);
}
.action-cell {
	display: flex;
	gap: 10px;
}
.danger-btn {
	background-color: #dc3545;
	color: white;
}
.modal-overlay {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 1000;
}
.modal-content {
	width: 100%;
	max-width: 500px;
	background: white;
}
</style>

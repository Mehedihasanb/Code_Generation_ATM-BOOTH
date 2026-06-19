<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';

const pendingCustomers = ref<any[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

const selectedCustomer = ref<any | null>(null);
const showModal = ref(false);
const dailyLimit = ref<number>(1000);
const absoluteLimit = ref<number>(0);

const fetchPendingCustomers = async () => {
    loading.value = true;
    error.value = null;
    try {
        const response = await authorizedFetch('/users?status=PENDING');

        if (!response.ok) throw new Error("Failed to fetch pending customers.");

        const pageData = await response.json();
        pendingCustomers.value = pageData.content || pageData.items || pageData;

    } catch (err) {
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        loading.value = false;
    }
};

const openReviewModal = (customer: any) => {
    selectedCustomer.value = customer;
    dailyLimit.value = 1000;
    absoluteLimit.value = 0;
    showModal.value = true;
};

const closeReviewModal = () => {
    selectedCustomer.value = null;
    showModal.value = false;
};

const submitApproval = async () => {
    if (!selectedCustomer.value) return;

    try {
        const response = await authorizedFetch(`/users/${selectedCustomer.value.id}`, {
            method: 'PATCH',
            body: JSON.stringify({
                status: 'ACTIVE',
                absoluteTransferLimit: Number(absoluteLimit.value),
                dailyTransferLimit: Number(dailyLimit.value),
            }),
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Approval failed (${response.status})`);
        }

        alert(`Customer ${selectedCustomer.value.firstName} approved! Accounts generated.`);
        closeReviewModal();
        await fetchPendingCustomers();
    } catch (err) {
        console.error(err);
        alert(`Failed to approve customer: ${err instanceof Error ? err.message : String(err)}`);
    }
};

const denyCustomer = async (id: number) => {
    if (!confirm("Are you sure you want to DENY this application?")) return;

    try {
        const response = await authorizedFetch(`/users/${id}`, {
            method: 'PATCH',
            body: JSON.stringify({ status: 'CLOSED' }),
        });

        if (!response.ok) throw new Error("Denial failed.");

        alert("Customer application denied.");
        await fetchPendingCustomers();
    } catch (err) {
        console.error(err);
        alert("Failed to deny customer.");
    }
};

onMounted(() => {
    fetchPendingCustomers();
});
</script>

<template>
    <main class="dashboard-wrapper">
        <header class="dashboard-header">
            <div>
                <h1>Pending Approvals</h1>
                <p class="muted">Review new customer registrations.</p>
            </div>
        </header>

        <section class="panel content-section">
            <p v-if="loading">Loading applications...</p>
            <p v-else-if="error" class="error">{{ error }}</p>
            <p v-else-if="pendingCustomers.length === 0" class="muted">No pending registrations at this time.</p>

            <div v-else class="table-container table-cards">
                <table class="data-table responsive-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Email</th>
                            <th>BSN</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="customer in pendingCustomers" :key="customer.id">
                            <td data-label="Name">{{ customer.firstName }} {{ customer.lastName }}</td>
                            <td data-label="Email" class="cell-email">{{ customer.email }}</td>
                            <td data-label="BSN">{{ customer.bsn || customer.bsnNumber || 'N/A' }}</td>
                            <td data-label="Actions" class="action-cell">
                                <button type="button" class="btn primary-btn" @click="openReviewModal(customer)">Review</button>
                                <button type="button" class="btn danger-btn" @click="denyCustomer(customer.id)">Deny</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <div v-if="showModal" class="modal-overlay">
            <div class="panel modal-content">
                <h2>Approve Customer</h2>
                <p class="muted">Approving <strong>{{ selectedCustomer?.firstName }} {{ selectedCustomer?.lastName }}</strong> will automatically generate their Checking and Savings accounts.</p>

                <form class="auth-form" @submit.prevent="submitApproval">
                    <label>
                        <span>Daily Transfer Limit (€)</span>
                        <input type="number" v-model.number="dailyLimit" required min="1" />
                        <small class="muted">Maximum amount that customer can transfer per day.</small>
                    </label>

                    <label>
                        <span>Absolute Account Limit (€)</span>
                        <input type="number" v-model.number="absoluteLimit" required />
                        <small class="muted">Account balance cannot exceed this amount.</small>
                    </label>

                    <div class="button-group">
                        <button type="button" class="btn secondary-btn" @click="closeReviewModal">Cancel</button>
                        <button type="submit" class="btn primary-btn">Approve & Create Accounts</button>
                    </div>
                </form>
            </div>
        </div>
    </main>
</template>

<style scoped>
.modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    padding: 1rem;
}
.modal-content {
    width: 100%;
    max-width: 28rem;
    max-height: 90vh;
    overflow-y: auto;
}
</style>

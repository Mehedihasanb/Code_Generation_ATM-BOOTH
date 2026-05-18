<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

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
        const response = await fetch('/users?hasAccount=false', {
            headers: { 'Authorization': `Bearer ${auth.token}` }
        });
        
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
        const response = await fetch('/accounts', {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${auth.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                customerRegistrationId: selectedCustomer.value.id,
                minimumAllowedBalance: absoluteLimit.value, 
                dailyOutgoingTransferLimit: dailyLimit.value
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "Approval failed.");
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
        // Matches the AuthController endpoint from earlier
        const response = await fetch(`/auth/customers/${id}/deny`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${auth.token}` }
        });

        if (!response.ok) throw new Error("Denial failed.");
        
        alert("Customer application denied.");
        await fetchPendingCustomers(); // Refresh the list
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
                        <th>BSN</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="customer in pendingCustomers" :key="customer.id">
                        <td>{{ customer.firstName }} {{ customer.lastName }}</td>
                        <td>{{ customer.email }}</td>
                        <td>{{ customer.bsnNumber }}</td>
                        <td class="action-cell">
                            <button class="btn primary-btn" @click="openReviewModal(customer)">Review</button>
                            <button class="btn danger-btn" @click="denyCustomer(customer.id)">Deny</button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </section>

        <div v-if="showModal" class="modal-overlay">
            <div class="panel modal-content">
                <h2>Approve Customer</h2>
                <p class="muted">Approving <strong>{{ selectedCustomer?.firstName }} {{ selectedCustomer?.lastName }}</strong> will automatically generate their Checking and Savings accounts.</p>
                
                <form class="auth-form" @submit.prevent="submitApproval">
                    <label>
                        <span>Daily Transfer Limit (€)</span>
                        <input type="number" v-model="dailyLimit" required min="1" />
                        <small class="muted">Maximum amount they can transfer per day.</small>
                    </label>

                    <label>
                        <span>Absolute Transfer Limit (€)</span>
                        <input type="number" v-model="absoluteLimit" required />
                        <small class="muted">Account balance cannot drop below this amount (can be negative for overdraft).</small>
                    </label>

                    <div class="button-group" style="margin-top: 20px;">
                        <button type="button" class="btn secondary-btn" @click="closeReviewModal">Cancel</button>
                        <button type="submit" class="btn primary-btn">Approve & Create Accounts</button>
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
.data-table th, .data-table td {
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
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.5);
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
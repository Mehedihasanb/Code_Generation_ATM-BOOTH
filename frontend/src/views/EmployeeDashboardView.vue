<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth'; 

const auth = useAuthStore();
const router = useRouter();

// UI State Management
const activeTab = ref('pending'); 

// Placeholder CRUD data and methods for Pending Approvals tab
const pendingRegistrations = ref([
    { id: 1, firstName: 'Fernando', lastName: 'Vázquez', email: 'fer@example.com' }
]);

const approveCustomer = async (id: number) => {
    try {
        // TODO: Connect this to your actual Pinia API store later
        /* 
        await fetch(`/api/registrations/${id}/approve`, { 
            method: 'PATCH',
            headers: { 'Authorization': `Bearer ${auth.token}` }
        });
        */
        alert(`Customer ${id} approved successfully!`);
        pendingRegistrations.value = pendingRegistrations.value.filter(c => c.id !== id);
    } catch (error) {
        console.error("Failed to approve customer", error);
    }
};
</script>

<template>
    <main class="dashboard-wrapper">
        <header class="dashboard-header">
            <div>
                <h1>Employee Portal</h1>
                <p class="muted">Welcome back, {{ auth.firstName || 'Colleague' }}</p>
            </div>
            <button class="btn secondary-btn" @click="router.push('/')">Back to Home</button>
        </header>

        <nav class="tabs panel">
            <button :class="{ active: activeTab === 'pending' }" @click="activeTab = 'pending'">Pending Approvals</button>
            <button :class="{ active: activeTab === 'customers' }" @click="activeTab = 'customers'">Customer Directory</button>
            <button :class="{ active: activeTab === 'transactions' }" @click="activeTab = 'transactions'">Global Transactions</button>
        </nav>

        <section v-if="activeTab === 'pending'" class="panel content-section">
            <h2>Pending Registrations (Update)</h2>
            <p class="muted" v-if="pendingRegistrations.length === 0">No pending registrations at this time.</p>
            
            <ul class="data-list" v-else>
                <li v-for="customer in pendingRegistrations" :key="customer.id" class="data-row">
                    <div class="user-info">
                        <strong>{{ customer.firstName }} {{ customer.lastName }}</strong>
                        <span class="muted">{{ customer.email }}</span>
                    </div>
                    <button class="btn primary-btn" @click="approveCustomer(customer.id)">Approve</button>
                </li>
            </ul>
        </section>

        <section v-if="activeTab === 'customers'" class="panel content-section">
            <h2>Customer Accounts (Read / Delete)</h2>
            <p class="muted">List of active customers will appear here. Employees can view details, set transfer limits, or close accounts.</p>

        </section>

        <section v-if="activeTab === 'transactions'" class="panel content-section">
            <h2>System Transactions (Read / Create)</h2>
            <p class="muted">Global transaction ledger. Employees can initiate transfers between customer accounts here.</p>
        </section>
    </main>
</template>
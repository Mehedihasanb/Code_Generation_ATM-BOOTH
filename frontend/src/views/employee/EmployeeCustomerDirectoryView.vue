<script setup lang="ts">
import { ref } from 'vue';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';

const searchFirst = ref('');
const searchLast = ref('');
const searchIban = ref('');
const searchResults = ref<any[]>([]);
const isSearching = ref(false);
const searchError = ref<string | null>(null);

const selectedUser = ref<any | null>(null);
const transactions = ref<any[]>([]);
const currentPage = ref(0);
const totalPages = ref(0);
const loadingTx = ref(false);
const txError = ref<string | null>(null);

const absoluteLimitInput = ref<number>(0);
const dailyLimitInput = ref<number>(0);
const limitsError = ref<string | null>(null);
const limitsSuccess = ref<string | null>(null);
const savingLimits = ref(false);

const searchCustomers = async () => {
    const firstName = searchFirst.value.trim();
    const lastName = searchLast.value.trim();
    const iban = searchIban.value.trim();

    if (!firstName && !lastName && !iban) {
        searchError.value = "Please enter a name or an IBAN.";
        return;
    }

    if ((firstName && !lastName) || (!firstName && lastName)) {
        searchError.value = "Please enter both first and last name, or use IBAN search.";
        return;
    }
    
    isSearching.value = true;
    searchError.value = null;
    selectedUser.value = null; 

    try {
        const params = new URLSearchParams();

        if (firstName && lastName) {
            params.set('firstName', firstName);
            params.set('lastName', lastName);
        }
        if (iban) {
            params.set('iban', iban);
        }

        const response = await authorizedFetch(`/users?${params.toString()}`);
        if (!response.ok) throw new Error("Search failed. Please check the backend.");

        const data = await response.json();
        searchResults.value = data.content ? data.content : data;
        if (searchResults.value.length === 0) {
            searchError.value = iban
                ? "No customers found for that IBAN."
                : "No customers found with that exact name.";
        }
    } catch (err) {
        searchError.value = err instanceof Error ? err.message : String(err);
    } finally {
        isSearching.value = false;
    }
};

const currentAbsoluteLimit = (user: { accounts?: { minimumAllowedBalance?: number }[] }) => {
    const accounts = user.accounts ?? [];
    if (accounts.length === 0) return 0;
    return Number(accounts[0].minimumAllowedBalance ?? 0);
};

const currentDailyLimit = (user: { accounts?: { dailyOutgoingTransferLimit?: number }[] }) => {
    const accounts = user.accounts ?? [];
    if (accounts.length === 0) return 0;
    return Number(accounts[0].dailyOutgoingTransferLimit ?? 0);
};

const viewUserHistory = async (user: any, pageIndex: number = 0) => {
    selectedUser.value = user;
    absoluteLimitInput.value = currentAbsoluteLimit(user);
    dailyLimitInput.value = currentDailyLimit(user);
    limitsError.value = null;
    limitsSuccess.value = null;
    loadingTx.value = true;
    txError.value = null;

    try {
        const response = await authorizedFetch(`/users/${user.id}/transactions?page=${pageIndex}&size=10`);
        if (!response.ok) throw new Error("Failed to load user transactions.");
        
        const pageData = await response.json();
        transactions.value = pageData.content || [];
        currentPage.value = pageData.number;   
        totalPages.value = pageData.totalPages;
    } catch (err) {
        txError.value = err instanceof Error ? err.message : String(err);
    } finally {
        loadingTx.value = false;
    }
};

const backToSearch = () => {
    selectedUser.value = null;
    transactions.value = [];
    limitsError.value = null;
    limitsSuccess.value = null;
};

const updateCustomerLimits = async () => {
    if (!selectedUser.value) return;

    savingLimits.value = true;
    limitsError.value = null;
    limitsSuccess.value = null;

    try {
        const response = await authorizedFetch(`/users/${selectedUser.value.id}/limits`, {
            method: 'PUT',
            body: JSON.stringify({
                absoluteLimit: Number(absoluteLimitInput.value),
                dailyOutgoingTransferLimit: Number(dailyLimitInput.value),
            }),
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || `Update failed (${response.status})`);
        }

        const updated = await response.json();
        absoluteLimitInput.value = Number(updated.absoluteLimit);
        dailyLimitInput.value = Number(updated.dailyOutgoingTransferLimit);
        limitsSuccess.value = `Limits updated on ${updated.accountsUpdated} account(s).`;

        if (selectedUser.value.accounts?.length) {
            selectedUser.value.accounts.forEach((account: {
                minimumAllowedBalance?: number;
                dailyOutgoingTransferLimit?: number;
            }) => {
                account.minimumAllowedBalance = updated.absoluteLimit;
                account.dailyOutgoingTransferLimit = updated.dailyOutgoingTransferLimit;
            });
        }
    } catch (err) {
        limitsError.value = err instanceof Error ? err.message : String(err);
    } finally {
        savingLimits.value = false;
    }
};

const formatCurrency = (amt: number) => {
    return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(amt);
};

const formatDate = (dateString: string) => {
    if (!dateString) return 'Unknown Date';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('nl-NL', { 
        day: 'numeric', month: 'short', year: 'numeric', 
        hour: '2-digit', minute: '2-digit'
    }).format(date);
};

const getTypeBadgeClass = (type: string) => {
    switch(type) {
        case 'DEPOSIT': return 'badge-deposit';
        case 'WITHDRAWAL': return 'badge-withdrawal';
        case 'TRANSFER': return 'badge-transfer';
        default: return 'badge-default';
    }
};
</script>

<template>
    <main class="home-wrapper">
        <section class="panel hero-section">
            <h1 class="headline">Customer Directory</h1>
            <p class="muted subtitle">Search for customers and view their specific transaction history.</p>
        </section>

        <section class="panel auth-panel" style="max-width: 1000px; margin: 0 auto;">
            
            <div v-if="!selectedUser">
                <div class="search-bar" style="display: flex; gap: 1rem; align-items: flex-end; margin-bottom: 2rem; background: #f8f9fa; padding: 1.5rem; border-radius: 8px;">
                    <label style="flex: 1; font-weight: bold; font-size: 0.9rem;">
                        First Name
                        <input type="text" v-model="searchFirst" placeholder="e.g. John" style="width: 100%; padding: 0.5rem; margin-top: 0.3rem;" @keyup.enter="searchCustomers" />
                    </label>
                    <label style="flex: 1; font-weight: bold; font-size: 0.9rem;">
                        Last Name
                        <input type="text" v-model="searchLast" placeholder="e.g. Doe" style="width: 100%; padding: 0.5rem; margin-top: 0.3rem;" @keyup.enter="searchCustomers" />
                    </label>
                    <label style="flex: 1; font-weight: bold; font-size: 0.9rem;">
                        IBAN
                        <input type="text" v-model="searchIban" placeholder="e.g. NL91INHO0000000001" style="width: 100%; padding: 0.5rem; margin-top: 0.3rem;" @keyup.enter="searchCustomers" />
                    </label>
                    <button class="btn" @click="searchCustomers" :disabled="isSearching" style="padding: 0.5rem 1.5rem; height: 38px;">
                        {{ isSearching ? 'Searching...' : 'Search' }}
                    </button>
                </div>

                <p v-if="searchError" class="error" style="color: #dc3545; font-weight: bold;">{{ searchError }}</p>

                <div v-if="searchResults.length > 0" class="table-container">
                    <table class="transaction-table" style="width: 100%; border-collapse: collapse; text-align: left;">
                        <thead>
                            <tr style="border-bottom: 2px solid #ccc;">
                                <th style="padding: 1rem;">ID</th>
                                <th style="padding: 1rem;">Name</th>
                                <th style="padding: 1rem;">Email</th>
                                <th style="padding: 1rem; text-align: right;">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="user in searchResults" :key="user.id" style="border-bottom: 1px solid #eee;">
                                <td style="padding: 1rem;" class="muted">#{{ user.id }}</td>
                                <td style="padding: 1rem; font-weight: bold;">{{ user.firstName }} {{ user.lastName }}</td>
                                <td style="padding: 1rem;">{{ user.email }}</td>
                                <td style="padding: 1rem; text-align: right;">
                                    <button class="btn secondary-btn" @click="viewUserHistory(user)">View History &rarr;</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div v-else>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                    <h2 style="margin: 0;">{{ selectedUser.firstName }} {{ selectedUser.lastName }}</h2>
                    <button class="btn secondary-btn" @click="backToSearch">&larr; Back to Search</button>
                </div>

                <section
                    v-if="selectedUser.accounts?.length"
                    class="limits-panel"
                    style="background: #f8f9fa; padding: 1.25rem; border-radius: 8px; margin-bottom: 1.5rem;">
                    <h3 style="margin: 0 0 0.75rem 0; font-size: 1rem;">Transfer limits</h3>
                    <p class="muted" style="margin: 0 0 1rem 0; font-size: 0.9rem;">
                        Applied to all of this customer's accounts (checking and savings). Changes take effect immediately.
                    </p>
                    <div style="display: flex; gap: 1rem; align-items: flex-end; flex-wrap: wrap;">
                        <label style="font-weight: bold; font-size: 0.9rem;">
                            Absolute limit (€)
                            <input
                                type="number"
                                v-model.number="absoluteLimitInput"
                                min="0"
                                step="0.01"
                                required
                                style="display: block; width: 200px; padding: 0.5rem; margin-top: 0.3rem;" />
                        </label>
                        <label style="font-weight: bold; font-size: 0.9rem;">
                            Daily limit (€)
                            <input
                                type="number"
                                v-model.number="dailyLimitInput"
                                min="0.01"
                                step="0.01"
                                required
                                style="display: block; width: 200px; padding: 0.5rem; margin-top: 0.3rem;" />
                        </label>
                        <button class="btn" @click="updateCustomerLimits" :disabled="savingLimits" style="height: 38px;">
                            {{ savingLimits ? 'Saving...' : 'Update limits' }}
                        </button>
                    </div>
                    <p v-if="limitsError" class="error" style="color: #dc3545; font-weight: bold; margin-top: 0.75rem;">{{ limitsError }}</p>
                    <p v-if="limitsSuccess" style="color: #155724; font-weight: bold; margin-top: 0.75rem;">{{ limitsSuccess }}</p>
                </section>
                <p v-else class="muted" style="margin-bottom: 1.5rem;">
                    This customer has no accounts yet; limits can be set when accounts are opened at the service desk.
                </p>

                <h3 style="margin: 0 0 1rem 0; font-size: 1rem;">Transaction history</h3>
                <p v-if="txError" class="error" style="color: #dc3545; font-weight: bold;">{{ txError }}</p>
                <p v-if="loadingTx" class="muted">Loading ledger...</p>

                <div v-else-if="transactions.length === 0" class="empty-state" style="text-align: center; padding: 2rem;">
                    <p class="muted">This customer has no transactions on record.</p>
                </div>

                <div v-else class="table-container">
                    <table class="transaction-table" style="width: 100%; border-collapse: collapse; text-align: left; font-size: 0.95rem;">
                        <thead>
                            <tr style="border-bottom: 2px solid #ccc;">
                                <th style="padding: 1rem 0.5rem;">Date</th>
                                <th style="padding: 1rem 0.5rem;">Type</th>
                                <th style="padding: 1rem 0.5rem;">From / To</th>
                                <th style="padding: 1rem 0.5rem; text-align: right;">Amount</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="tx in transactions" :key="tx.transactionId" style="border-bottom: 1px solid #eee;">
                                <td style="padding: 1rem 0.5rem;" class="muted">{{ formatDate(tx.timestamp) }}</td>
                                <td style="padding: 1rem 0.5rem;"><span :class="['type-badge', getTypeBadgeClass(tx.type)]">{{ tx.type }}</span></td>
                                <td style="padding: 1rem 0.5rem; font-family: monospace;">
                                    <div v-if="tx.type === 'DEPOSIT'">To: {{ tx.toIban }}</div>
                                    <div v-else-if="tx.type === 'WITHDRAWAL'">From: {{ tx.fromIban }}</div>
                                    <div v-else>
                                        <div style="font-size: 0.85rem; color: #666;">From: {{ tx.fromIban }}</div>
                                        <div>To: {{ tx.toIban }}</div>
                                    </div>
                                </td>
                                <td style="padding: 1rem 0.5rem; text-align: right; font-weight: bold;">{{ formatCurrency(tx.amount) }}</td>
                            </tr>
                        </tbody>
                    </table>

                    <div v-if="totalPages > 1" class="pagination-controls" style="display: flex; justify-content: space-between; align-items: center; margin-top: 2rem; padding-top: 1rem; border-top: 1px solid #eee;">
                        <button class="btn secondary-btn" :disabled="currentPage === 0 || loadingTx" @click="viewUserHistory(selectedUser, currentPage - 1)">&laquo; Previous</button>
                        <span class="muted" style="font-size: 0.9rem;">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
                        <button class="btn secondary-btn" :disabled="currentPage >= totalPages - 1 || loadingTx" @click="viewUserHistory(selectedUser, currentPage + 1)">Next &raquo;</button>
                    </div>
                </div>
            </div>

        </section>
    </main>
</template>

<style scoped>
.secondary-btn {
    background-color: #6c757d;
    color: white;
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 4px;
}
.secondary-btn:hover:not(:disabled) { background-color: #5a6268; }
.secondary-btn:disabled { background-color: #e9ecef; color: #6c757d; cursor: not-allowed; }

.transaction-table th { background-color: #f8f9fa; color: #495057; }
.transaction-table tr:hover { background-color: #f8f9fa; }

/* Badges */
.type-badge {
    padding: 0.25rem 0.5rem;
    border-radius: 12px;
    font-size: 0.8rem;
    font-weight: bold;
}
.badge-deposit { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
.badge-withdrawal { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
.badge-transfer { background-color: #cce5ff; color: #004085; border: 1px solid #b8daff; }
.badge-default { background-color: #e2e3e5; color: #383d41; }
</style>
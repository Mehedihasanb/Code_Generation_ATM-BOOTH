<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';
import AccountDeletionPanel from '@/components/organisms/AccountDeletionPanel.vue';

const searchFirst = ref('');
const searchLast = ref('');
const searchIban = ref('');
const customerList = ref<any[]>([]);
const listPage = ref(0);
const listTotalPages = ref(0);
const listTotalElements = ref(0);
const loadingList = ref(false);
const listError = ref<string | null>(null);
const isSearchMode = ref(false);
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
const showDeactivated = ref(false);

const loadAllCustomers = async (pageIndex: number = 0) => {
    loadingList.value = true;
    listError.value = null;
    isSearchMode.value = false;
    searchError.value = null;

    try {
        const params = new URLSearchParams({ page: String(pageIndex), size: '10' });
        if (showDeactivated.value) {
            params.set('deleted', 'true');
        }
        const response = await authorizedFetch(`/users?${params.toString()}`);
        if (!response.ok) throw new Error("Could not load customer directory.");

        const data = await response.json();
        customerList.value = data.content || [];
        listPage.value = data.number ?? pageIndex;
        listTotalPages.value = data.totalPages ?? 0;
        listTotalElements.value = data.totalElements ?? customerList.value.length;
    } catch (err) {
        listError.value = err instanceof Error ? err.message : String(err);
        customerList.value = [];
    } finally {
        loadingList.value = false;
    }
};

const searchCustomers = async () => {
    const firstName = searchFirst.value.trim();
    const lastName = searchLast.value.trim();
    const iban = searchIban.value.trim();

    if (!firstName && !lastName && !iban) {
        await loadAllCustomers(0);
        return;
    }

    if ((firstName && !lastName) || (!firstName && lastName)) {
        searchError.value = "Please enter both first and last name, or use IBAN search.";
        return;
    }
    
    isSearching.value = true;
    searchError.value = null;
    listError.value = null;
    selectedUser.value = null;
    isSearchMode.value = true;

    try {
        const params = new URLSearchParams();

        if (firstName && lastName) {
            params.set('firstName', firstName);
            params.set('lastName', lastName);
        }
        if (iban) {
            params.set('iban', iban);
        }
        if (showDeactivated.value) {
            params.set('deleted', 'true');
        }

        const response = await authorizedFetch(`/users?${params.toString()}`);
        if (!response.ok) throw new Error("Search failed. Please check the backend.");

        const data = await response.json();
        customerList.value = data.content ? data.content : data;
        listPage.value = 0;
        listTotalPages.value = 1;
        listTotalElements.value = customerList.value.length;
        if (customerList.value.length === 0) {
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

const clearSearch = async () => {
    searchFirst.value = '';
    searchLast.value = '';
    searchIban.value = '';
    await loadAllCustomers(0);
};

const toggleDeactivatedList = async () => {
    showDeactivated.value = !showDeactivated.value;
    selectedUser.value = null;
    searchFirst.value = '';
    searchLast.value = '';
    searchIban.value = '';
    await loadAllCustomers(0);
};

onMounted(() => {
    loadAllCustomers(0);
});

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

const handleCustomerDeleted = async () => {
    backToSearch();
    if (isSearchMode.value) {
        await searchCustomers();
    } else {
        await loadAllCustomers(listPage.value);
    }
};

const handleCustomerReactivated = async () => {
    backToSearch();
    showDeactivated.value = false;
    await loadAllCustomers(0);
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
            <p class="muted subtitle">View all customers with accounts, search by name or IBAN, and open transaction history.</p>
            <button
                type="button"
                class="btn secondary-btn"
                style="margin-top: 1rem;"
                @click="toggleDeactivatedList"
                :disabled="loadingList">
                {{ showDeactivated ? 'Show active customers' : 'Show deactivated accounts' }}
            </button>
        </section>

        <section class="panel page-panel-wide">
            
            <div v-if="!selectedUser">
                <div class="search-bar">
                    <label class="form-field">
                        First Name
                        <input type="text" v-model="searchFirst" placeholder="e.g. John" @keyup.enter="searchCustomers" />
                    </label>
                    <label class="form-field">
                        Last Name
                        <input type="text" v-model="searchLast" placeholder="e.g. Doe" @keyup.enter="searchCustomers" />
                    </label>
                    <label class="form-field">
                        IBAN
                        <input type="text" v-model="searchIban" placeholder="e.g. NL91INHO0000000001" @keyup.enter="searchCustomers" />
                    </label>
                    <button class="btn primary-btn" type="button" @click="searchCustomers" :disabled="isSearching || loadingList">
                        {{ isSearching ? 'Searching...' : 'Search' }}
                    </button>
                    <button v-if="isSearchMode" class="btn secondary-btn" type="button" @click="clearSearch" :disabled="loadingList">
                        Show all
                    </button>
                </div>

                <p v-if="searchError" class="error" style="color: #dc3545; font-weight: bold;">{{ searchError }}</p>
                <p v-if="listError" class="error" style="color: #dc3545; font-weight: bold;">{{ listError }}</p>
                <p v-if="loadingList" class="muted">Loading customers...</p>

                <div v-else-if="customerList.length > 0" class="table-container table-cards">
                    <p class="muted" style="margin-bottom: 1rem;">
                        {{ showDeactivated ? 'Deactivated customers' : isSearchMode ? 'Search results' : 'All customers' }} ({{ listTotalElements }} total)
                    </p>
                    <table class="transaction-table responsive-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Status</th>
                                <th>Accounts</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="user in customerList" :key="user.id">
                                <td data-label="ID" class="muted">#{{ user.id }}</td>
                                <td data-label="Name"><strong>{{ user.firstName }} {{ user.lastName }}</strong></td>
                                <td data-label="Email" class="cell-email">{{ user.email }}</td>
                                <td data-label="Status">
                                    <span v-if="user.deactivated">DEACTIVATED</span>
                                    <span v-else>{{ user.customerApprovalStatus || 'N/A' }}</span>
                                </td>
                                <td data-label="Accounts">
                                    <span v-if="!user.accounts?.length" class="muted">No accounts</span>
                                    <span v-else>{{ user.accounts.length }} account(s)</span>
                                </td>
                                <td data-label="Action" class="action-cell">
                                    <button class="btn secondary-btn" type="button" @click="viewUserHistory(user)">View History</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <div v-if="!isSearchMode && listTotalPages > 1" class="pagination-controls">
                        <button class="btn secondary-btn" :disabled="listPage === 0 || loadingList" @click="loadAllCustomers(listPage - 1)">&laquo; Previous</button>
                        <span class="muted" style="font-size: 0.9rem;">Page {{ listPage + 1 }} of {{ listTotalPages }}</span>
                        <button class="btn secondary-btn" :disabled="listPage >= listTotalPages - 1 || loadingList" @click="loadAllCustomers(listPage + 1)">Next &raquo;</button>
                    </div>
                </div>

                <div v-else-if="!loadingList && !searchError && !listError" class="empty-state" style="text-align: center; padding: 2rem;">
                    <p class="muted">No customers found.</p>
                </div>
            </div>

            <div v-else>
                <div class="detail-header">
                    <h2>{{ selectedUser.firstName }} {{ selectedUser.lastName }}</h2>
                    <button type="button" class="btn secondary-btn" @click="backToSearch">&larr; Back to Search</button>
                </div>

                <section
                    v-if="selectedUser.accounts?.length"
                    class="limits-panel">
                    <h3 style="margin: 0 0 0.75rem 0; font-size: 1rem;">Transfer limits</h3>
                    <p class="muted" style="margin: 0 0 1rem 0; font-size: 0.9rem;">
                        Applied to all of this customer's accounts (checking and savings). Changes take effect immediately.
                    </p>
                    <div class="limits-form-row">
                        <label class="form-field">
                            Absolute limit (€)
                            <input
                                type="number"
                                v-model.number="absoluteLimitInput"
                                min="0"
                                step="0.01"
                                required />
                        </label>
                        <label class="form-field">
                            Daily limit (€)
                            <input
                                type="number"
                                v-model.number="dailyLimitInput"
                                min="0.01"
                                step="0.01"
                                required />
                        </label>
                        <button class="btn primary-btn" type="button" @click="updateCustomerLimits" :disabled="savingLimits">
                            {{ savingLimits ? 'Saving...' : 'Update limits' }}
                        </button>
                    </div>
                    <p v-if="limitsError" class="error" style="color: #dc3545; font-weight: bold; margin-top: 0.75rem;">{{ limitsError }}</p>
                    <p v-if="limitsSuccess" style="color: #155724; font-weight: bold; margin-top: 0.75rem;">{{ limitsSuccess }}</p>
                </section>
                <p v-else class="muted" style="margin-bottom: 1.5rem;">
                    This customer has no accounts yet; limits can be set when accounts are opened at the service desk.
                </p>

                <AccountDeletionPanel
                    mode="employee"
                    :target-user-id="selectedUser.id"
                    :target-user-name="`${selectedUser.firstName} ${selectedUser.lastName}`"
                    :deactivated="selectedUser.deactivated"
                    @deleted="handleCustomerDeleted"
                    @reactivated="handleCustomerReactivated" />

                <h3 style="margin: 0 0 1rem 0; font-size: 1rem;">Transaction history</h3>
                <p v-if="txError" class="error" style="color: #dc3545; font-weight: bold;">{{ txError }}</p>
                <p v-if="loadingTx" class="muted">Loading ledger...</p>

                <div v-else-if="transactions.length === 0" class="empty-state" style="text-align: center; padding: 2rem;">
                    <p class="muted">This customer has no transactions on record.</p>
                </div>

                <div v-else class="table-container table-cards">
                    <table class="transaction-table responsive-table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Type</th>
                                <th>From / To</th>
                                <th>Amount</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="tx in transactions" :key="tx.transactionId">
                                <td data-label="Date" class="muted">{{ formatDate(tx.timestamp) }}</td>
                                <td data-label="Type"><span :class="['type-badge', getTypeBadgeClass(tx.type)]">{{ tx.type }}</span></td>
                                <td data-label="From / To" class="cell-mono">
                                    <div v-if="tx.type === 'DEPOSIT'">To: {{ tx.toIban }}</div>
                                    <div v-else-if="tx.type === 'WITHDRAWAL'">From: {{ tx.fromIban }}</div>
                                    <div v-else>
                                        <div style="font-size: 0.85rem; color: #666;">From: {{ tx.fromIban }}</div>
                                        <div>To: {{ tx.toIban }}</div>
                                    </div>
                                </td>
                                <td data-label="Amount" style="font-weight: bold;">{{ formatCurrency(tx.amount) }}</td>
                            </tr>
                        </tbody>
                    </table>

                    <div v-if="totalPages > 1" class="pagination-controls">
                        <button class="btn secondary-btn" :disabled="currentPage === 0 || loadingTx" @click="viewUserHistory(selectedUser, currentPage - 1)">&laquo; Previous</button>
                        <span class="muted" style="font-size: 0.9rem;">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
                        <button class="btn secondary-btn" :disabled="currentPage >= totalPages - 1 || loadingTx" @click="viewUserHistory(selectedUser, currentPage + 1)">Next &raquo;</button>
                    </div>
                </div>
            </div>

        </section>
    </main>
</template>

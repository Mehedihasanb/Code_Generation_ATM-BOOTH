<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';

const searchQuery = ref('');
const customerList = ref<any[]>([]);
const listPage = ref(0);
const listTotalPages = ref(0);
const listTotalElements = ref(0);
const loadingList = ref(false);
const listError = ref<string | null>(null);
const isSearchMode = ref(false);
const showClosed = ref(false);

const selectedUser = ref<any | null>(null);
const employeeAccounts = ref<any[]>([]);
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

const loadAllCustomers = async (pageIndex: number = 0) => {
    loadingList.value = true;
    listError.value = null;
    isSearchMode.value = false;

    try {
        const params = new URLSearchParams({ page: String(pageIndex), size: '10' });
        if (showClosed.value) {
            params.set('status', 'CLOSED');
        } else {
            params.set('status', 'ACTIVE');
        }
        const response = await authorizedFetch(`/users?${params.toString()}`);
        if (!response.ok) throw new Error('Could not load customers.');

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
    const query = searchQuery.value.trim();
    if (!query) {
        await loadAllCustomers(0);
        return;
    }

    loadingList.value = true;
    listError.value = null;
    selectedUser.value = null;
    isSearchMode.value = true;

    try {
        const params = new URLSearchParams({ search: query, size: '50' });
        if (showClosed.value) {
            params.set('status', 'CLOSED');
        }
        const response = await authorizedFetch(`/users?${params.toString()}`);
        if (!response.ok) throw new Error('Search failed.');

        const data = await response.json();
        customerList.value = data.content || [];
        listPage.value = 0;
        listTotalPages.value = 1;
        listTotalElements.value = customerList.value.length;
        if (customerList.value.length === 0) {
            listError.value = 'No customers found.';
        }
    } catch (err) {
        listError.value = err instanceof Error ? err.message : String(err);
    } finally {
        loadingList.value = false;
    }
};

const clearSearch = async () => {
    searchQuery.value = '';
    await loadAllCustomers(0);
};

const toggleClosedList = async () => {
    showClosed.value = !showClosed.value;
    selectedUser.value = null;
    searchQuery.value = '';
    await loadAllCustomers(0);
};

onMounted(() => {
    loadAllCustomers(0);
});

const loadEmployeeAccounts = async (userId: number) => {
    const response = await authorizedFetch(`/accounts?userId=${userId}&size=10`);
    if (!response.ok) throw new Error('Could not load account limits.');
    const data = await response.json();
    employeeAccounts.value = data.content || [];
    if (employeeAccounts.value.length > 0) {
        absoluteLimitInput.value = Number(employeeAccounts.value[0].absoluteTransferLimit ?? 0);
        dailyLimitInput.value = Number(employeeAccounts.value[0].dailyTransferLimit ?? 0);
    }
};

const viewUserHistory = async (user: any, pageIndex: number = 0) => {
    selectedUser.value = user;
    limitsError.value = null;
    limitsSuccess.value = null;
    loadingTx.value = true;
    txError.value = null;

    try {
        const detailResponse = await authorizedFetch(`/users/${user.id}`);
        if (!detailResponse.ok) throw new Error('Failed to load customer detail.');
        selectedUser.value = await detailResponse.json();

        await loadEmployeeAccounts(user.id);

        const response = await authorizedFetch(`/transactions?customerId=${user.id}&page=${pageIndex}&size=10`);
        if (!response.ok) throw new Error('Failed to load user transactions.');

        const pageData = await response.json();
        transactions.value = (pageData.content || []).map((tx: any) => ({
            ...tx,
            transactionId: tx.id,
        }));
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
    employeeAccounts.value = [];
    transactions.value = [];
    limitsError.value = null;
    limitsSuccess.value = null;
};

const updateCustomerLimits = async () => {
    if (!selectedUser.value || employeeAccounts.value.length === 0) return;

    savingLimits.value = true;
    limitsError.value = null;
    limitsSuccess.value = null;

    try {
        const body = JSON.stringify({
            absoluteTransferLimit: Number(absoluteLimitInput.value),
            dailyTransferLimit: Number(dailyLimitInput.value),
        });

        for (const account of employeeAccounts.value) {
            const response = await authorizedFetch(`/accounts/${account.iban}`, {
                method: 'PATCH',
                body,
            });
            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || `Update failed (${response.status})`);
            }
        }

        limitsSuccess.value = `Limits updated on ${employeeAccounts.value.length} account(s).`;
        await loadEmployeeAccounts(selectedUser.value.id);
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
        hour: '2-digit', minute: '2-digit',
    }).format(date);
};

const getTypeBadgeClass = (type: string) => {
    switch (type) {
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
            <p class="muted subtitle">View customers, search by name, and open transaction history.</p>
            <button
                type="button"
                class="btn secondary-btn"
                style="margin-top: 1rem;"
                @click="toggleClosedList"
                :disabled="loadingList">
                {{ showClosed ? 'Show active customers' : 'Show closed customers' }}
            </button>
        </section>

        <section class="panel page-panel-wide">
            <div v-if="!selectedUser">
                <div class="search-bar">
                    <label class="form-field">
                        Search
                        <input type="text" v-model="searchQuery" placeholder="Name or email" @keyup.enter="searchCustomers" />
                    </label>
                    <button class="btn primary-btn" type="button" @click="searchCustomers" :disabled="loadingList">
                        Search
                    </button>
                    <button v-if="isSearchMode" class="btn secondary-btn" type="button" @click="clearSearch" :disabled="loadingList">
                        Show all
                    </button>
                </div>

                <p v-if="listError" class="error" style="color: #dc3545; font-weight: bold;">{{ listError }}</p>
                <p v-if="loadingList" class="muted">Loading customers...</p>

                <div v-else-if="customerList.length > 0" class="table-container table-cards">
                    <p class="muted" style="margin-bottom: 1rem;">
                        {{ showClosed ? 'Closed customers' : isSearchMode ? 'Search results' : 'Active customers' }} ({{ listTotalElements }} total)
                    </p>
                    <table class="transaction-table responsive-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="user in customerList" :key="user.id">
                                <td data-label="ID" class="muted">#{{ user.id }}</td>
                                <td data-label="Name"><strong>{{ user.firstName }} {{ user.lastName }}</strong></td>
                                <td data-label="Email" class="cell-email">{{ user.email }}</td>
                                <td data-label="Status">{{ user.status || 'N/A' }}</td>
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

                <div v-else-if="!loadingList && !listError" class="empty-state" style="text-align: center; padding: 2rem;">
                    <p class="muted">No customers found.</p>
                </div>
            </div>

            <div v-else>
                <div class="detail-header">
                    <h2>{{ selectedUser.firstName }} {{ selectedUser.lastName }}</h2>
                    <button type="button" class="btn secondary-btn" @click="backToSearch">&larr; Back to Search</button>
                </div>

                <section v-if="employeeAccounts.length" class="limits-panel">
                    <h3 style="margin: 0 0 0.75rem 0; font-size: 1rem;">Transfer limits</h3>
                    <p class="muted" style="margin: 0 0 1rem 0; font-size: 0.9rem;">
                        Applied to all of this customer's accounts via PATCH /accounts/{iban}.
                    </p>
                    <div class="limits-form-row">
                        <label class="form-field">
                            Absolute limit (€)
                            <input type="number" v-model.number="absoluteLimitInput" min="0" step="0.01" required />
                        </label>
                        <label class="form-field">
                            Daily limit (€)
                            <input type="number" v-model.number="dailyLimitInput" min="0.01" step="0.01" required />
                        </label>
                        <button class="btn primary-btn" type="button" @click="updateCustomerLimits" :disabled="savingLimits">
                            {{ savingLimits ? 'Saving...' : 'Update limits' }}
                        </button>
                    </div>
                    <p v-if="limitsError" class="error" style="color: #dc3545; font-weight: bold; margin-top: 0.75rem;">{{ limitsError }}</p>
                    <p v-if="limitsSuccess" style="color: #155724; font-weight: bold; margin-top: 0.75rem;">{{ limitsSuccess }}</p>
                </section>
                <p v-else class="muted" style="margin-bottom: 1.5rem;">
                    This customer has no accounts yet. Activate them via PATCH /users/{id} with status ACTIVE.
                </p>

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

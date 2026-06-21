<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';
import { fetchMyAccounts as loadMyAccounts } from '@/composables/useMyAccounts';

const route = useRoute();

const myAccounts = ref<any[]>([]);
const selectedIban = ref('');
const transactions = ref<any[]>([]);

const currentPage = ref(0);
const totalPages = ref(0);
const loading = ref(true);
const error = ref<string | null>(null);

const filters = reactive({
    startDate: '',
    endDate: '',
    amount: '',
    amountOperator: 'eq',
    counterpartIban: ''
});

const fetchMyAccounts = async () => {
    loading.value = true;
    try {
        const summary = await loadMyAccounts();
        myAccounts.value = summary.accounts;

        const queryIban = typeof route.query.accountIban === 'string' ? route.query.accountIban : '';
        const matchingAccount = myAccounts.value.find((acc) => acc.iban === queryIban);
        if (matchingAccount) {
            selectedIban.value = matchingAccount.iban;
        } else if (myAccounts.value.length > 0) {
            selectedIban.value = myAccounts.value[0].iban;
        }

        if (selectedIban.value) {
            await fetchTransactions(0);
        }
    } catch (err) {
        error.value = "Failed to load accounts.";
    } finally {
        loading.value = false;
    }
};

const fetchTransactions = async (pageIndex: number) => {
    if (!selectedIban.value) return;
    
    loading.value = true;
    error.value = null;

    try {
        // Build the base URL
        let url = `/transactions?accountIban=${selectedIban.value}&page=${pageIndex}&size=10`;
        
        // Append filters if they have been filled in
        if (filters.startDate) url += `&startDate=${filters.startDate}`;
        if (filters.endDate) url += `&endDate=${filters.endDate}`;
        if (filters.amount) {
            url += `&amount=${filters.amount}&amountOperator=${filters.amountOperator}`;
        }
        if (filters.counterpartIban) {
            const compactIban = filters.counterpartIban.replace(/\s/g, '').toUpperCase();
            url += `&counterpartIban=${encodeURIComponent(compactIban)}`;
        }

        const response = await authorizedFetch(url);
        if (!response.ok) throw new Error("Failed to load transactions.");
        
        const pageData = await response.json();
        
        transactions.value = (pageData.content || []).map((tx: any) => ({
            ...tx,
            transactionId: tx.id,
            type: tx.toIban === selectedIban.value ? 'INCOMING' : 'OUTGOING',
            counterpartIban: tx.toIban === selectedIban.value ? tx.fromIban : tx.toIban,
        }));
        currentPage.value = pageData.number;   
        totalPages.value = pageData.totalPages;

    } catch (err) {
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        loading.value = false;
    }
};

const clearFilters = () => {
    filters.startDate = '';
    filters.endDate = '';
    filters.amount = '';
    filters.amountOperator = 'eq';
    filters.counterpartIban = '';
    fetchTransactions(0); 
};

watch(selectedIban, (newIban) => {
    if (newIban) {
        // Clean the filters when switching acc
        filters.startDate = '';
        filters.endDate = '';
        filters.amount = '';
        filters.amountOperator = 'eq';
        filters.counterpartIban = '';
        fetchTransactions(0);
    }
});

const formatCurrency = (amt: number, type: string) => {
    const formatted = new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(amt);
    return type === 'INCOMING' ? `+ ${formatted}` : `- ${formatted}`;
};

const formatDate = (dateString: string) => {
    if (!dateString) return 'Unknown Date';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('nl-NL', { 
        day: 'numeric', month: 'short', year: 'numeric', 
        hour: '2-digit', minute: '2-digit' 
    }).format(date);
};

onMounted(() => {
    fetchMyAccounts();
});
</script>

<template>
    <main class="home-wrapper">
        <section class="panel hero-section">
            <h1 class="headline">Transaction History</h1>
            <p class="muted subtitle">View all your previous activity.</p>
        </section>

        <section class="panel page-panel-wide">
            
            <div class="account-selector">
                <label class="form-field">
                    <span>Select Account</span>
                    <select v-model="selectedIban" :disabled="loading">
                        <option disabled value="">Select an account</option>
                        <option v-for="acc in myAccounts" :key="acc.iban" :value="acc.iban">
                            {{ acc.accountType }} - {{ acc.iban }}
                        </option>
                    </select>
                </label>
            </div>

            <div class="filter-bar">
                <label class="filter-field">
                    From
                    <input type="date" v-model="filters.startDate" />
                </label>
                
                <label class="filter-field">
                    To
                    <input type="date" v-model="filters.endDate" />
                </label>

                <div class="filter-amount-row">
                    <label class="filter-field">
                        Amount
                        <select v-model="filters.amountOperator">
                            <option value="eq">Equal to</option>
                            <option value="gt">Greater than</option>
                            <option value="lt">Less than</option>
                        </select>
                    </label>
                    <input type="number" v-model="filters.amount" placeholder="0.00" step="0.01" />
                </div>

                <label class="filter-field">
                    Receiver IBAN
                    <input type="text" v-model="filters.counterpartIban" placeholder="Partial IBAN, e.g. NL44" />
                </label>

                <div class="filter-actions">
                    <button type="button" class="btn primary-btn" @click="fetchTransactions(0)">Apply Filters</button>
                    <button type="button" class="btn secondary-btn" @click="clearFilters">Clear</button>
                </div>
            </div>

            <p v-if="error" class="error" style="color: #dc3545; font-weight: bold;">{{ error }}</p>

            <p v-if="loading && transactions.length === 0" class="muted">Loading transactions...</p>

            <div v-else-if="transactions.length === 0 && !loading" class="empty-state" style="text-align: center; padding: 2rem;">
                <p class="muted">No transactions found matching your criteria.</p>
            </div>

            <div v-else class="table-container table-cards">
                <table class="transaction-table responsive-table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Description</th>
                            <th>IBAN</th>
                            <th>Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="tx in transactions" :key="tx.transactionId">
                            <td data-label="Date" class="muted">{{ formatDate(tx.timestamp) }}</td>
                            <td data-label="Description" :title="tx.description">
                                {{ tx.description || 'No description' }}
                            </td>
                            <td data-label="IBAN" class="cell-mono">
                                <span v-if="tx.type === 'INCOMING'">From: </span>
                                <span v-else>To: </span>
                                {{ tx.counterpartIban }}
                            </td>
                            <td data-label="Amount" style="font-weight: bold;"
                                :class="tx.type === 'INCOMING' ? 'text-success' : 'text-danger'">
                                {{ formatCurrency(tx.amount, tx.type) }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="totalPages > 1" class="pagination-controls">
                <button 
                    class="btn secondary-btn" 
                    :disabled="currentPage === 0 || loading" 
                    @click="fetchTransactions(currentPage - 1)">
                    &laquo; Previous
                </button>
                
                <span class="muted" style="font-size: 0.9rem;">
                    Page {{ currentPage + 1 }} of {{ totalPages }}
                </span>
                
                <button 
                    class="btn secondary-btn" 
                    :disabled="currentPage >= totalPages - 1 || loading" 
                    @click="fetchTransactions(currentPage + 1)">
                    Next &raquo;
                </button>
            </div>

        </section>
    </main>
</template>

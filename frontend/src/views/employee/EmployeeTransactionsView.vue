<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';

const transactions = ref<any[]>([]);
const currentPage = ref(0);
const totalPages = ref(0);
const loading = ref(true);
const error = ref<string | null>(null);

const filters = reactive({
    startDate: '',
    endDate: '',
    amount: '',
    amountOperator: 'eq'
});

const fetchAllTransactions = async (pageIndex: number) => {
    loading.value = true;
    error.value = null;

    try {
        // Added: &sort=timestamp,desc forces Spring Boot to order by Date (newest first)
        let url = `/transactions?page=${pageIndex}&size=10&sort=timestamp,desc`;
        
        // Append filters if they have been filled in
        if (filters.startDate) url += `&startDate=${filters.startDate}`;
        if (filters.endDate) url += `&endDate=${filters.endDate}`;
        if (filters.amount) {
            url += `&amount=${filters.amount}&amountOperator=${filters.amountOperator}`;
        }
        

        const response = await authorizedFetch(url);
        
        if (!response.ok) {
            let errorMessage = "Failed to load system transactions.";
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (e) {
                errorMessage = `Error: ${response.status} ${response.statusText}`;
            }
            throw new Error(errorMessage);
        }
        
        const pageData = await response.json();
        transactions.value = (pageData.content || []).map((tx: any) => ({
            ...tx,
            transactionId: tx.id,
            initiatingUser: tx.initiatingUserEmail,
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
    fetchAllTransactions(0); 
};

const formatCurrency = (amt: number) => {
    return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(amt);
};

const formatDate = (dateString: string) => {
    if (!dateString) return 'Unknown Date';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('nl-NL', { 
        day: 'numeric', month: 'short', year: 'numeric', 
        hour: '2-digit', minute: '2-digit', second: '2-digit'
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

onMounted(() => {
    fetchAllTransactions(0);
});
</script>

<template>
    <main class="home-wrapper">
        <section class="panel hero-section">
            <h1 class="headline">Global Transaction History</h1>
            <p class="muted subtitle">Monitor all bank transactions. Use system with responsibility. Privacy first.</p>
        </section>

        <section class="panel page-panel-xl">
            
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

                <div class="filter-actions">
                    <button type="button" class="btn secondary-btn" @click="clearFilters">Clear</button>
                    <button type="button" class="btn primary-btn" @click="fetchAllTransactions(0)">Apply Filters</button>
                </div>
            </div>

            <p v-if="error" class="error" style="color: #dc3545; font-weight: bold;">{{ error }}</p>
            <p v-if="loading && transactions.length === 0" class="muted">Loading system...</p>

            <div v-else-if="transactions.length === 0 && !loading" class="empty-state" style="text-align: center; padding: 2rem;">
                <p class="muted">No transactions found matching your criteria.</p>
            </div>

            <div v-else class="table-container table-cards">
                <table class="transaction-table data-table responsive-table">
                    <thead>
                        <tr>
                            <th>Timestamp</th>
                            <th>Type</th>
                            <th>Sender</th>
                            <th>Receiver</th>
                            <th>Initiating User</th>
                            <th>Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="tx in transactions" :key="tx.transactionId">
                            <td data-label="Time" class="muted">{{ formatDate(tx.timestamp) }}</td>
                            <td data-label="Type">
                                <span :class="['type-badge', getTypeBadgeClass(tx.type)]">{{ tx.type }}</span>
                            </td>
                            <td data-label="Sender" class="cell-mono">{{ tx.fromIban || '-' }}</td>
                            <td data-label="Receiver" class="cell-mono">{{ tx.toIban || '-' }}</td>
                            <td data-label="User" class="cell-email muted">{{ tx.initiatingUser }}</td>
                            <td data-label="Amount" style="font-weight: bold;">{{ formatCurrency(tx.amount) }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="totalPages > 1" class="pagination-controls">
                <button class="btn secondary-btn" :disabled="currentPage === 0 || loading" @click="fetchAllTransactions(currentPage - 1)">&laquo; Previous</button>
                <span class="muted" style="font-size: 0.9rem;">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
                <button class="btn secondary-btn" :disabled="currentPage >= totalPages - 1 || loading" @click="fetchAllTransactions(currentPage + 1)">Next &raquo;</button>
            </div>

        </section>
    </main>
</template>

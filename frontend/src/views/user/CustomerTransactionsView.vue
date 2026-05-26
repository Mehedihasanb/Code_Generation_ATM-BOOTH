<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';

const myAccounts = ref<any[]>([]);
const selectedIban = ref('');
const transactions = ref<any[]>([]);

const currentPage = ref(0);
const totalPages = ref(0);
const loading = ref(true);
const error = ref<string | null>(null);

const fetchMyAccounts = async () => {
    loading.value = true;
    try {
        const response = await authorizedFetch('/accounts/mine');
        if (!response.ok) throw new Error("Could not load your accounts.");
        
        const data = await response.json();
        myAccounts.value = data.accounts || [];
        
        if (myAccounts.value.length > 0) {
            selectedIban.value = myAccounts.value[0].iban;
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
        const response = await authorizedFetch(`/transactions?accountIban=${selectedIban.value}&page=${pageIndex}&size=10`);
        if (!response.ok) throw new Error("Failed to load transactions.");
        
        const pageData = await response.json();
        
        transactions.value = pageData.content || [];
        currentPage.value = pageData.number;   
        totalPages.value = pageData.totalPages;

    } catch (err) {
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        loading.value = false;
    }
};

watch(selectedIban, (newIban) => {
    if (newIban) fetchTransactions(0);
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
            <p class="muted subtitle">View your paginated ledger.</p>
        </section>

        <section class="panel auth-panel" style="max-width: 800px; margin: 0 auto;">
            
            <div class="account-selector" style="margin-bottom: 2rem;">
                <label>
                    <span style="font-weight: bold; margin-right: 1rem;">Select Account:</span>
                    <select v-model="selectedIban" :disabled="loading" style="padding: 0.5rem; border-radius: 4px; border: 1px solid #ccc; width: 60%; max-width: 400px;">
                        <option disabled value="">Select an account</option>
                        <option v-for="acc in myAccounts" :key="acc.iban" :value="acc.iban">
                            {{ acc.accountType }} - {{ acc.iban }}
                        </option>
                    </select>
                </label>
            </div>

            <p v-if="error" class="error" style="color: #dc3545; font-weight: bold;">{{ error }}</p>

            <p v-if="loading && transactions.length === 0" class="muted">Loading transactions...</p>

            <div v-else-if="transactions.length === 0 && !loading" class="empty-state" style="text-align: center; padding: 2rem;">
                <p class="muted">No transactions found for this account.</p>
            </div>

            <div v-else class="table-container" style="overflow-x: auto;">
                <table class="transaction-table" style="width: 100%; border-collapse: collapse; text-align: left;">
                    <thead>
                        <tr style="border-bottom: 2px solid var(--border-color, #ccc);">
                            <th style="padding: 1rem 0.5rem;">Date</th>
                            <th style="padding: 1rem 0.5rem;">Description</th>
                            <th style="padding: 1rem 0.5rem;">Counterpart IBAN</th>
                            <th style="padding: 1rem 0.5rem; text-align: right;">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="tx in transactions" :key="tx.transactionId" style="border-bottom: 1px solid #eee;">
                            <td style="padding: 1rem 0.5rem; white-space: nowrap;" class="muted">{{ formatDate(tx.timestamp) }}</td>
                            <td style="padding: 1rem 0.5rem; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="tx.description">
                                {{ tx.description || 'No description' }}
                            </td>
                            <td style="padding: 1rem 0.5rem; font-family: monospace; font-size: 0.9rem;">
                                <span v-if="tx.type === 'INCOMING'">From: </span>
                                <span v-else>To: </span><br>
                                {{ tx.counterpartIban }}
                            </td>
                            <td style="padding: 1rem 0.5rem; text-align: right; font-weight: bold;" 
                                :class="tx.type === 'INCOMING' ? 'text-success' : 'text-danger'">
                                {{ formatCurrency(tx.amount, tx.type) }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="totalPages > 1" class="pagination-controls" style="display: flex; justify-content: space-between; align-items: center; margin-top: 2rem; padding-top: 1rem; border-top: 1px solid #eee;">
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

<style scoped>
.text-success { color: #28a745; }
.text-danger { color: #dc3545; }
.secondary-btn {
    background-color: #6c757d;
    color: white;
    padding: 0.5rem 1rem;
}
.secondary-btn:disabled {
    background-color: #e9ecef;
    color: #6c757d;
    cursor: not-allowed;
}
.transaction-table th {
    background-color: #f8f9fa;
    color: #495057;
}
.transaction-table tr:hover {
    background-color: #f8f9fa;
}
</style>
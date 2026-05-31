<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';

const transactions = ref<any[]>([]);
const currentPage = ref(0);
const totalPages = ref(0);
const loading = ref(true);
const error = ref<string | null>(null);

const fetchAllTransactions = async (pageIndex: number) => {
    loading.value = true;
    error.value = null;

    try {
        // Not passing an accountIban here because the backend will detect our employee role and return the full system transactions instead of filtering by account
        const response = await authorizedFetch(`/transactions?page=${pageIndex}&size=10`);
        
        if (!response.ok) {
            throw new Error("Failed to load system transactions.");
        }
        
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

// Helper to color-code the transaction types
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
            <h1 class="headline">System Transaction Control</h1>
            <p class="muted subtitle">Monitor all bank transactions. Use system with responsability as actions are irreversible.</p>
        </section>

        <section class="panel auth-panel" style="max-width: 1100px; margin: 0 auto;">
            
            <p v-if="error" class="error" style="color: #dc3545; font-weight: bold;">{{ error }}</p>
            <p v-if="loading && transactions.length === 0" class="muted">Loading system...</p>

            <div v-else-if="transactions.length === 0 && !loading" class="empty-state" style="text-align: center; padding: 2rem;">
                <p class="muted">No transactions exist in the system yet.</p>
            </div>

            <div v-else class="table-container" style="overflow-x: auto;">
                <table class="transaction-table" style="width: 100%; border-collapse: collapse; text-align: left; font-size: 0.95rem;">
                    <thead>
                        <tr style="border-bottom: 2px solid var(--border-color, #ccc);">
                            <th style="padding: 1rem 0.5rem;">Timestamp</th>
                            <th style="padding: 1rem 0.5rem;">Type</th>
                            <th style="padding: 1rem 0.5rem;">Sender</th>
                            <th style="padding: 1rem 0.5rem;">Receiver</th>
                            <th style="padding: 1rem 0.5rem;">Initiating User</th>
                            <th style="padding: 1rem 0.5rem; text-align: right;">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="tx in transactions" :key="tx.transactionId" style="border-bottom: 1px solid #eee;">
                            <td style="padding: 1rem 0.5rem; white-space: nowrap;" class="muted">{{ formatDate(tx.timestamp) }}</td>
                            
                            <td style="padding: 1rem 0.5rem;">
                                <span :class="['type-badge', getTypeBadgeClass(tx.type)]">{{ tx.type }}</span>
                            </td>

                            <td style="padding: 1rem 0.5rem; font-family: monospace;">
                                {{ tx.fromIban }}
                            </td>

                            <td style="padding: 1rem 0.5rem; font-family: monospace;">
                                {{ tx.toIban }}
                            </td>

                            <td style="padding: 1rem 0.5rem; color: #495057;">
                                {{ tx.initiatingUser }}
                            </td>

                            <td style="padding: 1rem 0.5rem; text-align: right; font-weight: bold;">
                                {{ formatCurrency(tx.amount) }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="totalPages > 1" class="pagination-controls" style="display: flex; justify-content: space-between; align-items: center; margin-top: 2rem; padding-top: 1rem; border-top: 1px solid #eee;">
                <button class="btn secondary-btn" :disabled="currentPage === 0 || loading" @click="fetchAllTransactions(currentPage - 1)">&laquo; Previous</button>
                <span class="muted" style="font-size: 0.9rem;">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
                <button class="btn secondary-btn" :disabled="currentPage >= totalPages - 1 || loading" @click="fetchAllTransactions(currentPage + 1)">Next &raquo;</button>
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

/* Transaction Types */
.type-badge {
    padding: 0.25rem 0.5rem;
    border-radius: 12px;
    font-size: 0.8rem;
    font-weight: bold;
    letter-spacing: 0.5px;
}
.badge-deposit { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
.badge-withdrawal { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
.badge-transfer { background-color: #cce5ff; color: #004085; border: 1px solid #b8daff; }
.badge-default { background-color: #e2e3e5; color: #383d41; }
</style>
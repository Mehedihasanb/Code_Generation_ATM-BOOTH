<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();

const loading = ref(true);
const error = ref<string | null>(null);

type AccountDetail = {
    iban: string;
    accountType: 'CHECKING' | 'SAVINGS';
    balance: number;
    absoluteLimit: number;
};

type AccountSummaryResponse = {
    customerName: string;
    combinedBalance: number;
    accounts: AccountDetail[];
};

const accountSummary = ref<AccountSummaryResponse | null>(null);

const fetchMyAccounts = async () => {
    loading.value = true;
    error.value = null;
    
    try {
        // Grab token from Pinia or directly from sessionStorage
        const currentToken = auth.token || sessionStorage.getItem('code-generation-token');
        
        if (!currentToken) {
            throw new Error("No authorization token found. Please log in again.");
        }

        const response = await fetch('/accounts/mine', {
            headers: { 
                'Authorization': `Bearer ${currentToken}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            const errText = await response.text();
            throw new Error(errText || `Failed to load accounts (${response.status})`);
        }
        
        accountSummary.value = await response.json();
    } catch (err) {
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        loading.value = false;
    }
};

const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(amount);
};

onMounted(() => {
    fetchMyAccounts();
});
</script>

<template>
    <main class="home-wrapper">
        <section v-if="loading" class="panel hero-section">
            <h2 class="headline">Loading your accounts...</h2>
        </section>

        <section v-else-if="error" class="panel hero-section">
            <h2 class="headline" style="color: #dc3545;">Unable to load accounts</h2>
            <p class="muted">{{ error }}</p>
            <button class="btn primary-btn" @click="fetchMyAccounts">Try Again</button>
        </section>

        <template v-else-if="accountSummary">
            <section class="panel hero-section">
                <h1 class="headline">{{ formatCurrency(accountSummary.combinedBalance) }}</h1>
                <p class="muted subtitle">
                    Total Combined Balance for {{ accountSummary.customerName || auth.firstName }}
                </p>
            </section>

            <section class="features-grid">
                <article 
                    v-for="account in accountSummary.accounts" 
                    :key="account.iban" 
                    class="panel feature-card"
                >
                    <div class="icon">
                        {{ account.accountType === 'CHECKING' ? '💳' : '📈' }}
                    </div>
                    <h3>{{ account.accountType === 'CHECKING' ? 'Checking Account' : 'Savings Account' }}</h3>
                    
                    <div class="account-details">
                        <p class="balance">{{ formatCurrency(account.balance) }}</p>
                        <p class="iban muted">IBAN: {{ account.iban }}</p>
                    </div>

                    <div class="button-group" style="margin-top: 1rem;">
                        <button class="btn primary-btn" style="width: 100%; padding: 0.5rem;">View History</button>
                    </div>
                </article>
            </section>
        </template>
    </main>
</template>

<style scoped>
.account-details {
    margin: 1.5rem 0;
    text-align: center;
}
.balance {
    font-size: 2rem;
    font-weight: bold;
    color: var(--text-color, #2c3e50);
    margin-bottom: 0.5rem;
}
.iban {
    font-family: monospace;
    font-size: 0.9rem;
    background: #f8f9fa;
    padding: 0.3rem 0.6rem;
    border-radius: 4px;
    display: inline-block;
}
</style>
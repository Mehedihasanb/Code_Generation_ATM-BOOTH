<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { fetchMyAccounts } from '@/composables/useMyAccounts';

const auth = useAuthStore();

const loading = ref(true);
const error = ref<string | null>(null);

const customerName = ref('');
const combinedBalance = ref(0);
const accounts = ref<Awaited<ReturnType<typeof fetchMyAccounts>>['accounts']>([]);

const fetchAccounts = async () => {
    loading.value = true;
    error.value = null;
    
    try {
        const summary = await fetchMyAccounts();
        accounts.value = summary.accounts;
        combinedBalance.value = summary.combinedBalance;
        customerName.value = auth.firstName ?? '';
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
    fetchAccounts();
});
</script>

<template>
    <main class="home-wrapper">
        <section v-if="loading" class="panel hero-section">
            <h2 class="headline">Loading your accounts...</h2>
        </section>

        <section v-else-if="error" class="panel hero-section">
            <h2 class="headline text-danger">Unable to load accounts</h2>
            <p class="muted">{{ error }}</p>
            <button class="btn primary-btn" @click="fetchAccounts">Try Again</button>
        </section>

        <template v-else>
            <section class="panel hero-section">
                <h1 class="headline">{{ formatCurrency(combinedBalance) }}</h1>
                <p class="muted subtitle">
                    Total Combined Balance for {{ customerName || auth.firstName }}
                </p>
            </section>

            <section class="features-grid">
                <article 
                    v-for="account in accounts" 
                    :key="account.iban" 
                    class="panel feature-card"
                >
                    <div class="feature-icon">
                        {{ account.accountType === 'CHECKING' ? 'CH' : 'SV' }}
                    </div>
                    <h3>{{ account.accountType === 'CHECKING' ? 'Checking Account' : 'Savings Account' }}</h3>
                    
                    <div class="account-details">
                        <p class="balance">{{ formatCurrency(account.balance) }}</p>
                        <p class="iban muted">IBAN: {{ account.iban }}</p>
                    </div>

                    <div class="button-group">
                        <button class="btn secondary-btn" type="button">View History</button>
                    </div>
                </article>
            </section>
        </template>
    </main>
</template>
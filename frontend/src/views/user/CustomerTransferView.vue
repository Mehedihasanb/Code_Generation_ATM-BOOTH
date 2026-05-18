<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();

const myAccounts = ref<any[]>([]);
const loadingAccounts = ref(true);
const submitting = ref(false);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);

const fromIban = ref('');
const toIban = ref('');
const amount = ref<number | ''>('');
const description = ref('');

const fetchMyAccounts = async () => {
    try {
        const currentToken = auth.token || sessionStorage.getItem('code-generation-token');
        const response = await fetch('/accounts/mine', {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        if (!response.ok) throw new Error("Could not load your accounts.");
        
        const data = await response.json();
        myAccounts.value = data.accounts || [];
        
        if (myAccounts.value.length > 0) {
            fromIban.value = myAccounts.value[0].iban;
        }
    } catch (err) {
        error.value = "Failed to load accounts for transfer.";
    } finally {
        loadingAccounts.value = false;
    }
};

const submitTransfer = async () => {
    error.value = null;
    successMessage.value = null;

    if (!fromIban.value || !toIban.value) {
        error.value = "Please select both a sender and receiver account.";
        return;
    }
    if (fromIban.value === toIban.value) {
        error.value = "You cannot transfer money to the same account.";
        return;
    }
    if (!amount.value || amount.value <= 0) {
        error.value = "Please enter a valid amount greater than 0.";
        return;
    }

    submitting.value = true;

    try {
        const currentToken = auth.token || sessionStorage.getItem('code-generation-token');
        
        const response = await fetch('/transactions', {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${currentToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                fromIban: fromIban.value,
                toIban: toIban.value,
                amount: amount.value,
                description: description.value || "Internal Transfer"
            })
        });

        if (!response.ok) {
            const errText = await response.text();
            throw new Error(errText || "Transfer failed. Please check your balance and limits.");
        }

        successMessage.value = `Successfully transferred €${amount.value} to ${toIban.value}!`;
        
        // Reset the form
        toIban.value = '';
        amount.value = '';
        description.value = '';

    } catch (err) {
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        submitting.value = false;
    }
};

const formatCurrency = (amt: number) => {
    return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(amt);
};

onMounted(() => {
    fetchMyAccounts();
});
</script>

<template>
    <main class="home-wrapper">
        <section class="panel hero-section">
            <h1 class="headline">Transfer Funds</h1>
            <p class="muted subtitle">Move money between your accounts securely.</p>
        </section>

        <section class="panel auth-panel" style="max-width: 600px; margin: 0 auto;">
            <p v-if="loadingAccounts" class="muted">Loading your accounts...</p>
            
            <form v-else class="auth-form" @submit.prevent="submitTransfer">
                
                <label>
                    <span>From Account</span>
                    <select v-model="fromIban" required>
                        <option disabled value="">Select an account</option>
                        <option v-for="acc in myAccounts" :key="acc.iban" :value="acc.iban">
                            {{ acc.accountType }} - {{ acc.iban }} ({{ formatCurrency(acc.balance) }})
                        </option>
                    </select>
                </label>

                <label>
                    <span>To Account (Internal)</span>
                    <select v-model="toIban" required>
                        <option disabled value="">Select destination</option>
                        <option v-for="acc in myAccounts" :key="acc.iban" :value="acc.iban">
                            {{ acc.accountType }} - {{ acc.iban }}
                        </option>
                    </select>
                </label>

                <label>
                    <span>Amount (€)</span>
                    <input type="number" v-model="amount" min="0.01" step="0.01" required placeholder="0.00" />
                </label>

                <label>
                    <span>Description (Optional)</span>
                    <input type="text" v-model="description" placeholder="e.g., Savings for vacation" maxlength="50" />
                </label>

                <p v-if="error" class="error" style="color: #dc3545; font-weight: bold;">{{ error }}</p>
                <p v-if="successMessage" class="success" style="color: #28a745; font-weight: bold;">{{ successMessage }}</p>

                <button class="btn primary-btn" type="submit" :disabled="submitting" style="margin-top: 1rem;">
                    {{ submitting ? 'Processing...' : 'Confirm Transfer' }}
                </button>
            </form>
        </section>
    </main>
</template>

<style scoped>
select {
    width: 100%;
    padding: 0.8rem;
    margin-top: 0.5rem;
    border: 1px solid var(--border-color, #ccc);
    border-radius: 4px;
    font-size: 1rem;
    background-color: white;
}
</style>
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';
import { useRouter } from 'vue-router';

const router = useRouter();
const myAccounts = ref<any[]>([]);
const loadingAccounts = ref(true);
const submitting = ref(false);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);
const transferType = ref<'INTERNAL' | 'EXTERNAL'>('INTERNAL');
const fromIban = ref('');
const toIban = ref('');
const amount = ref<number | ''>('');
const description = ref('');
const searchFirstName = ref('');
const searchLastName = ref('');
const searchIban = ref('');
const searchResults = ref<any[]>([]);
const searching = ref(false);


const fetchMyAccounts = async () => {
    try {
        const response = await authorizedFetch('/accounts/mine');
        
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

const searchDirectory = async () => {
    const firstName = searchFirstName.value.trim();
    const lastName = searchLastName.value.trim();
    const iban = searchIban.value.trim();

    if (!firstName && !lastName && !iban) {
        error.value = "Please enter a name or an IBAN to search.";
        return;
    }

    if ((firstName && !lastName) || (!firstName && lastName)) {
        error.value = "Please enter both first and last name, or use IBAN search.";
        return;
    }

    searching.value = true;
    searchResults.value = [];
    error.value = null;
    successMessage.value = null;

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
        if (!response.ok) throw new Error("Failed to search directory.");
        const data = await response.json();

        searchResults.value = data.content ? data.content : data;
        if (searchResults.value.length === 0) {
            error.value = iban
                ? "No active accounts found for that IBAN."
                : "No active accounts found for that name.";
        }
    } catch (err) {
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        searching.value = false;
    }
};

// Select an account from the search results
const selectExternalAccount = (iban: string) => {
    toIban.value = iban;
    successMessage.value = `Selected Recipient IBAN: ${iban}`;
    error.value = null;
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
        const response = await authorizedFetch('/transactions', {
            method: 'POST',
            body: JSON.stringify({
                fromIban: fromIban.value,
                toIban: toIban.value,
                amount: amount.value,
                description: description.value || "Transfer"
            })
        });

       if (!response.ok) {
            let errorMessage = "Transfer failed.";
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (e) {
                errorMessage = `Error: ${response.status} ${response.statusText}`;
            }
            throw new Error(errorMessage);
        }

        successMessage.value = `Successfully transferred €${amount.value} to ${toIban.value}!`;
        
        setTimeout(() => {
            router.push('/transactions');
        }, 1500);

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
            <p class="muted subtitle">Move money securely.</p>
        </section>

        <section class="panel auth-panel">
            
            <div class="toggle-group">
                <button 
                    type="button"
                    class="btn" 
                    :class="transferType === 'INTERNAL' ? 'primary-btn' : 'secondary-btn'"
                    @click="transferType = 'INTERNAL'; toIban = ''; error = null; successMessage = null"
                >
                    My Accounts
                </button>
                <button 
                    type="button"
                    class="btn" 
                    :class="transferType === 'EXTERNAL' ? 'primary-btn' : 'secondary-btn'"
                    @click="transferType = 'EXTERNAL'; toIban = ''; error = null; successMessage = null"
                >
                    Another Customer
                </button>
            </div>

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

                <label v-if="transferType === 'INTERNAL'">
                    <span>To Account (Internal)</span>
                    <select v-model="toIban" required>
                        <option disabled value="">Select destination</option>
                        <option v-for="acc in myAccounts" :key="acc.iban" :value="acc.iban">
                            {{ acc.accountType }} - {{ acc.iban }}
                        </option>
                    </select>
                </label>

                <div v-if="transferType === 'EXTERNAL'" class="external-search-box">
                    <p style="margin-bottom: 0.5rem; font-weight: bold;">Find Recipient</p>
                    <div class="external-search-row">
                        <input type="text" v-model="searchFirstName" placeholder="First Name" />
                        <input type="text" v-model="searchLastName" placeholder="Last Name" />
                        <input type="text" v-model="searchIban" placeholder="IBAN (optional)" />
                        <button type="button" class="btn secondary-btn" @click="searchDirectory" :disabled="searching">
                            {{ searching ? '...' : 'Search' }}
                        </button>
                    </div>

                    <div v-if="searchResults.length > 0" class="results-list">
                        <p class="muted" style="font-size: 0.9rem; margin-bottom: 0.5rem;">Select an account:</p>
                        
                        <div v-for="user in searchResults" :key="user.id" style="margin-bottom: 1rem;">
                            <strong style="display: block; margin-bottom: 0.5rem;">{{ user.firstName }} {{ user.lastName }}</strong>
                            
                            <button 
                                v-for="acc in user.accounts" 
                                :key="acc.iban"
                                type="button"
                                class="btn recipient-picker-btn"
                                @click="selectExternalAccount(acc.iban)"
                            >
                                <span style="font-weight: bold; color: var(--color-primary);">{{ acc.accountType }}</span><br>
                                <span class="muted" style="font-family: monospace;">{{ acc.iban }}</span>
                            </button>
                            
                            <p v-if="user.accounts.length === 0" class="muted" style="font-size: 0.85rem;">No active accounts available.</p>
                        </div>
                        <div v-if="error" class="alert-error">{{ error }}</div>

                        <div v-if="successMessage" class="alert-success">{{ successMessage }}</div>
                    </div>
                    </div>
                
                <label>
                    <span>Amount (€)</span>
                    <input type="number" v-model="amount" min="0.01" step="0.01" required placeholder="0.00" />
                </label>

                <label>
                    <span>Description (Optional)</span>
                    <input type="text" v-model="description" placeholder="e.g., Dinner last night" maxlength="50" />
                </label>
                
                <button class="btn primary-btn" type="submit" :disabled="submitting || !toIban" style="margin-top: 1rem;">
                    {{ submitting ? 'Processing...' : 'Confirm Transfer' }}
                </button>
            </form>
        </section>
    </main>
</template>

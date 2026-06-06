<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';

// --- STATE ---
const checkingAccounts = ref<any[]>([]);

const form = reactive({
    fromIban: '',
    toIban: '',
    amount: '',
    description: 'Manual Transfer via Support'
});

const loading = ref(false);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);

// Autocomplete State
const searchFrom = ref('');
const searchTo = ref('');
const showFromDropdown = ref(false);
const showToDropdown = ref(false);

// --- COMPUTED: Real-time filtering ---
const filteredFromAccounts = computed(() => {
    const q = searchFrom.value.toLowerCase();
    return checkingAccounts.value.filter(acc => 
        acc.ownerName.toLowerCase().includes(q) || acc.iban.toLowerCase().includes(q)
    );
});

const filteredToAccounts = computed(() => {
    const q = searchTo.value.toLowerCase();
    return checkingAccounts.value.filter(acc => 
        acc.ownerName.toLowerCase().includes(q) || acc.iban.toLowerCase().includes(q)
    );
});

// --- METHODS ---
const fetchCheckingAccounts = async () => {
    try {
        const response = await authorizedFetch('/accounts/checking-options');
        if (!response.ok) throw new Error("Failed to load account list.");
        checkingAccounts.value = await response.json();
    } catch (err) {
        error.value = "Warning: Could not load the accounts. Please refresh the page.";
    }
};

// Selection Handlers
const selectFromAccount = (acc: any) => {
    form.fromIban = acc.iban;
    searchFrom.value = `${acc.ownerName} (${acc.iban})`; // Fill input with full text
    showFromDropdown.value = false;
};

const selectToAccount = (acc: any) => {
    form.toIban = acc.iban;
    searchTo.value = `${acc.ownerName} (${acc.iban})`; // Fill input with full text
    showToDropdown.value = false;
};

const submitTransfer = async () => {
    // Basic Frontend Validation
    if (!form.fromIban || !form.toIban || !form.amount) {
        error.value = "Please select accounts from the dropdown and enter an amount.";
        successMessage.value = null;
        return;
    }
    
    if (form.fromIban === form.toIban) {
        error.value = "Sender and Receiver accounts cannot be the same.";
        successMessage.value = null;
        return;
    }

    if (parseFloat(form.amount) <= 0) {
        error.value = "Amount must be greater than zero.";
        successMessage.value = null;
        return;
    }

    loading.value = true;
    error.value = null;
    successMessage.value = null;

    try {
        const response = await authorizedFetch('/transactions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fromIban: form.fromIban,
                toIban: form.toIban,
                amount: parseFloat(form.amount),
                description: form.description.trim()
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
        successMessage.value = `Successfully transferred €${parseFloat(form.amount).toFixed(2)} from ${form.fromIban} to ${form.toIban}.`;
        
        // Reset the form and search bars
        form.fromIban = '';
        form.toIban = '';
        form.amount = '';
        form.description = 'Manual Transfer via Support';
        searchFrom.value = '';
        searchTo.value = '';

    } catch (err) {
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    fetchCheckingAccounts();
});
</script>

<template>
    <main class="home-wrapper">
        <section class="panel hero-section">
            <h1 class="headline">Force Transfer</h1>
            <p class="muted subtitle">Administrative action: Move funds between customer checking accounts.</p>
        </section>

        <section class="panel auth-panel" style="max-width: 600px; margin: 0 auto; padding: 2rem;">
            
            <div v-if="successMessage" class="success-banner">
                ✅ {{ successMessage }}
            </div>

            <div v-if="error" class="error-banner">
                ❌ {{ error }}
            </div>

            <form @submit.prevent="submitTransfer" style="display: flex; flex-direction: column; gap: 1.5rem;">
                
                <!-- SENDER AUTOCOMPLETE -->
                <label style="display: flex; flex-direction: column; font-weight: bold; color: #495057; position: relative;">
                    Sender Account
                    <input 
                        type="text" 
                        v-model="searchFrom" 
                        @focus="showFromDropdown = true"
                        @blur="showFromDropdown = false"
                        @input="form.fromIban = ''" 
                        placeholder="Search by name or IBAN..." 
                        required 
                        class="search-input" 
                    />
                    <ul v-if="showFromDropdown && filteredFromAccounts.length > 0" class="dropdown-list">
                        <li 
                            v-for="acc in filteredFromAccounts" 
                            :key="acc.iban" 
                            @mousedown.prevent="selectFromAccount(acc)"
                        >
                            <span style="font-weight: bold;">{{ acc.ownerName }}</span> <br/>
                            <span class="muted" style="font-size: 0.85rem; font-family: monospace;">{{ acc.iban }}</span>
                        </li>
                    </ul>
                    <ul v-if="showFromDropdown && filteredFromAccounts.length === 0" class="dropdown-list">
                        <li class="muted">No accounts found.</li>
                    </ul>
                </label>

                <!-- RECEIVER AUTOCOMPLETE -->
                <label style="display: flex; flex-direction: column; font-weight: bold; color: #495057; position: relative;">
                    Receiver Account
                    <input 
                        type="text" 
                        v-model="searchTo" 
                        @focus="showToDropdown = true"
                        @blur="showToDropdown = false"
                        @input="form.toIban = ''" 
                        placeholder="Search by name or IBAN..." 
                        required 
                        class="search-input" 
                    />
                    <ul v-if="showToDropdown && filteredToAccounts.length > 0" class="dropdown-list">
                        <li 
                            v-for="acc in filteredToAccounts" 
                            :key="acc.iban" 
                            @mousedown.prevent="selectToAccount(acc)"
                        >
                            <span style="font-weight: bold;">{{ acc.ownerName }}</span> <br/>
                            <span class="muted" style="font-size: 0.85rem; font-family: monospace;">{{ acc.iban }}</span>
                        </li>
                    </ul>
                    <ul v-if="showToDropdown && filteredToAccounts.length === 0" class="dropdown-list">
                        <li class="muted">No accounts found.</li>
                    </ul>
                </label>

                <div style="display: flex; gap: 1rem;">
                    <label style="display: flex; flex-direction: column; font-weight: bold; color: #495057; flex: 1;">
                        Amount (€)
                        <input type="number" v-model="form.amount" step="0.01" min="0.01" placeholder="0.00" required style="padding: 0.75rem; margin-top: 0.5rem; border: 1px solid #ced4da; border-radius: 4px; font-size: 1rem;" />
                    </label>

                    <label style="display: flex; flex-direction: column; font-weight: bold; color: #495057; flex: 2;">
                        Description
                        <input type="text" v-model="form.description" placeholder="Reason for transfer" required style="padding: 0.75rem; margin-top: 0.5rem; border: 1px solid #ced4da; border-radius: 4px; font-size: 1rem;" />
                    </label>
                </div>

                <button type="submit" class="btn" :disabled="loading" style="padding: 1rem; font-size: 1.1rem; font-weight: bold; margin-top: 1rem; background-color: #dc3545; border: none; border-radius: 4px; color: white; cursor: pointer;">
                    {{ loading ? 'Processing...' : 'Execute Transfer' }}
                </button>
                <p class="muted" style="text-align: center; font-size: 0.85rem; margin-top: 0;">This action is logged and cannot be undone.</p>
            </form>

        </section>
    </main>
</template>

<style scoped>
/* Basic Form Styling */
.success-banner { background-color: #d4edda; color: #155724; padding: 1rem; border-radius: 4px; margin-bottom: 1.5rem; font-weight: bold; border: 1px solid #c3e6cb; }
.error-banner { background-color: #f8d7da; color: #721c24; padding: 1rem; border-radius: 4px; margin-bottom: 1.5rem; font-weight: bold; border: 1px solid #f5c6cb; }
.search-input { padding: 0.75rem; margin-top: 0.5rem; border: 1px solid #ced4da; border-radius: 4px; font-size: 1rem; background-color: white; }

/* The Autocomplete Dropdown Box */
.dropdown-list {
    position: absolute;
    top: 100%; /* Positions it directly below the input */
    left: 0;
    right: 0;
    max-height: 200px;
    overflow-y: auto;
    background-color: white;
    border: 1px solid #ced4da;
    border-radius: 0 0 4px 4px;
    margin: 0;
    padding: 0;
    list-style: none;
    z-index: 1000;
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

/* Individual Dropdown Items */
.dropdown-list li {
    padding: 0.75rem;
    border-bottom: 1px solid #f1f3f5;
    cursor: pointer;
    transition: background-color 0.2s;
}
.dropdown-list li:last-child { border-bottom: none; }
.dropdown-list li:hover { background-color: #f8f9fa; }

/* Danger Button */
.btn:hover:not(:disabled) { background-color: #c82333 !important; }
.btn:disabled { background-color: #e9ecef !important; color: #6c757d !important; cursor: not-allowed; }
</style>
<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue';
import {
    fetchCheckingAccountOptions,
    submitEmployeeTransfer,
} from '@/composables/useEmployeeTransfer';

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

const searchFrom = ref('');
const searchTo = ref('');
const showFromDropdown = ref(false);
const showToDropdown = ref(false);

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

const fetchCheckingAccounts = async () => {
    try {
        checkingAccounts.value = await fetchCheckingAccountOptions();
    } catch (err) {
        error.value = 'Warning: Could not load the accounts. Please refresh the page.';
    }
};

const selectFromAccount = (acc: any) => {
    form.fromIban = acc.iban;
    searchFrom.value = `${acc.ownerName} (${acc.iban})`; 
    showFromDropdown.value = false;
};

const selectToAccount = (acc: any) => {
    form.toIban = acc.iban;
    searchTo.value = `${acc.ownerName} (${acc.iban})`; 
    showToDropdown.value = false;
};

const submitTransfer = async () => {
    loading.value = true;
    error.value = null;
    successMessage.value = null;

    try {
        await submitEmployeeTransfer({
            fromIban: form.fromIban,
            toIban: form.toIban,
            amount: parseFloat(form.amount),
            description: form.description.trim(),
        });
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
            <p class="muted subtitle">Administrative action: Move funds between customer checking accounts. Use with caution, actions are irreversible.</p>
            <p class="muted subtitle"> Customer approval is required.</p>
        </section>

        <section class="panel auth-panel">
            
            <div v-if="successMessage" class="alert-success">{{ successMessage }}</div>

            <div v-if="error" class="alert-error">{{ error }}</div>

            <form class="auth-form" @submit.prevent="submitTransfer">
                
                <label class="field-relative">
                    <span>Sender Account</span>
                    <input 
                        type="text" 
                        v-model="searchFrom" 
                        @focus="showFromDropdown = true"
                        @blur="showFromDropdown = false"
                        @input="form.fromIban = ''" 
                        placeholder="Search by name or IBAN..." 
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

                <label class="field-relative">
                    <span>Receiver Account</span>
                    <input 
                        type="text" 
                        v-model="searchTo" 
                        @focus="showToDropdown = true"
                        @blur="showToDropdown = false"
                        @input="form.toIban = ''" 
                        placeholder="Search by name or IBAN..." 
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

                <label class="form-field">
                    <span>Amount (€)</span>
                    <input type="number" v-model="form.amount" step="0.01" placeholder="0.00" />
                </label>

                <label class="form-field">
                    <span>Description</span>
                    <input type="text" v-model="form.description" placeholder="Reason for transfer" />
                </label>

                <button type="submit" class="btn danger-btn" :disabled="loading">
                    {{ loading ? 'Processing...' : 'Execute Transfer' }}
                </button>
                <p class="muted" style="text-align: center; font-size: 0.85rem;">This action is logged and cannot be undone.</p>
            </form>

        </section>
    </main>
</template>

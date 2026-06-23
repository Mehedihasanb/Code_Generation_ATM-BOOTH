<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { fetchMyAccounts as loadMyAccounts } from '@/composables/useMyAccounts';
import {
    type TransferTarget,
    normalizeSearchQuery,
    searchTransferTargets,
    submitCustomerTransfer,
} from '@/composables/useCustomerTransfer';

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
const searchName = ref('');
const searchResults = ref<TransferTarget[]>([]);
const searching = ref(false);
const showRecipientDropdown = ref(false);
const selectedRecipient = ref<TransferTarget | null>(null);

let searchTimer: ReturnType<typeof setTimeout> | null = null;
let latestSearchId = 0;

function recipientLabel(target: TransferTarget): string {
    return `${target.firstName} ${target.lastName}`;
}

const fetchMyAccounts = async () => {
    try {
        const summary = await loadMyAccounts();
        myAccounts.value = summary.accounts;

        if (myAccounts.value.length > 0) {
            fromIban.value = myAccounts.value[0].iban;
        }
    } catch (err) {
        error.value = 'Failed to load accounts for transfer.';
    } finally {
        loadingAccounts.value = false;
    }
};

const fetchRecipients = async (query: string) => {
    const searchId = ++latestSearchId;
    searching.value = true;
    error.value = null;

    try {
        searchResults.value = await searchTransferTargets(query);
        showRecipientDropdown.value = true;
    } catch (err) {
        if (searchId !== latestSearchId) {
            return;
        }
        searchResults.value = [];
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        if (searchId === latestSearchId) {
            searching.value = false;
        }
    }
};

watch(searchName, (value) => {
    const query = normalizeSearchQuery(value);

    if (
        selectedRecipient.value &&
        value !== recipientLabel(selectedRecipient.value) &&
        value !== selectedRecipient.value.iban
    ) {
        selectedRecipient.value = null;
        toIban.value = '';
        successMessage.value = null;
    }

    if (searchTimer) {
        clearTimeout(searchTimer);
    }

    if (!query) {
        searchResults.value = [];
        showRecipientDropdown.value = false;
        return;
    }

    showRecipientDropdown.value = true;
    searchTimer = setTimeout(() => {
        fetchRecipients(query);
    }, 250);
});

function onRecipientInput() {
    showRecipientDropdown.value = true;
}

function onRecipientFocus() {
    showRecipientDropdown.value = true;
    const query = normalizeSearchQuery(searchName.value);
    if (query && searchResults.value.length === 0) {
        fetchRecipients(query);
    }
}

function onRecipientBlur() {
    window.setTimeout(() => {
        showRecipientDropdown.value = false;
    }, 200);
}

function selectExternalAccount(target: TransferTarget) {
    selectedRecipient.value = target;
    toIban.value = target.iban;
    searchName.value = target.iban;
    successMessage.value = `Selected ${recipientLabel(target)} (${target.iban})`;
    error.value = null;
    showRecipientDropdown.value = false;
}

function resetExternalRecipient() {
    searchName.value = '';
    searchResults.value = [];
    toIban.value = '';
    selectedRecipient.value = null;
    showRecipientDropdown.value = false;
}

function switchTransferType(type: 'INTERNAL' | 'EXTERNAL') {
    transferType.value = type;
    toIban.value = '';
    error.value = null;
    successMessage.value = null;
    if (type === 'INTERNAL') {
        resetExternalRecipient();
    }
}

const submitTransfer = async () => {
    error.value = null;
    successMessage.value = null;
    submitting.value = true;

    try {
        await submitCustomerTransfer({
            fromIban: fromIban.value,
            toIban: toIban.value,
            amount: amount.value,
            description: description.value,
        });

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
                    @click="switchTransferType('INTERNAL')">
                    My Accounts
                </button>
                <button
                    type="button"
                    class="btn"
                    :class="transferType === 'EXTERNAL' ? 'primary-btn' : 'secondary-btn'"
                    @click="switchTransferType('EXTERNAL')">
                    Another Customer
                </button>
            </div>

            <p v-if="loadingAccounts" class="muted">Loading your accounts...</p>

            <form v-else class="auth-form" @submit.prevent="submitTransfer">
                <div v-if="error" class="alert-error">{{ error }}</div>
                <div v-if="successMessage" class="alert-success">{{ successMessage }}</div>

                <label>
                    <span>From Account</span>
                    <select v-model="fromIban">
                        <option disabled value="">Select an account</option>
                        <option v-for="acc in myAccounts" :key="acc.iban" :value="acc.iban">
                            {{ acc.accountType }} - {{ acc.iban }} ({{ formatCurrency(acc.balance) }})
                        </option>
                    </select>
                </label>

                <label v-if="transferType === 'INTERNAL'">
                    <span>To Account (Internal)</span>
                    <select v-model="toIban">
                        <option disabled value="">Select destination</option>
                        <option v-for="acc in myAccounts" :key="acc.iban" :value="acc.iban">
                            {{ acc.accountType }} - {{ acc.iban }}
                        </option>
                    </select>
                </label>

                <label v-if="transferType === 'EXTERNAL'" class="field-relative">
                    <span>Find Recipient</span>
                    <input
                        v-model="searchName"
                        type="text"
                        class="search-input"
                        placeholder="Name (e.g. Carol Jansen) or IBAN (e.g. NL44...)"
                        autocomplete="off"
                        @input="onRecipientInput"
                        @focus="onRecipientFocus"
                        @blur="onRecipientBlur" />
                    <p v-if="selectedRecipient" class="muted" style="font-size: 0.85rem; margin: 0.35rem 0 0;">
                        Recipient set: {{ recipientLabel(selectedRecipient) }} ({{ selectedRecipient.iban }})
                    </p>
                    <ul
                        v-if="showRecipientDropdown && (searchResults.length > 0 || searching)"
                        class="dropdown-list recipient-dropdown"
                        @mousedown.prevent>
                        <li v-if="searching" class="recipient-option-status muted">Searching...</li>
                        <li
                            v-for="target in searchResults"
                            :key="target.iban"
                            class="recipient-option-row">
                            <div class="recipient-option-details">
                                <span class="recipient-option-name">{{ target.firstName }} {{ target.lastName }}</span>
                                <span class="muted recipient-option-iban">{{ target.iban }}</span>
                            </div>
                            <button
                                type="button"
                                class="btn secondary-btn recipient-select-btn"
                                @mousedown.prevent="selectExternalAccount(target)">
                                Select
                            </button>
                        </li>
                    </ul>
                    <ul
                        v-else-if="showRecipientDropdown && normalizeSearchQuery(searchName) && !searching"
                        class="dropdown-list recipient-dropdown">
                        <li class="recipient-option-status muted">No customers found.</li>
                    </ul>
                </label>

                <label>
                    <span>Amount (€)</span>
                    <input v-model="amount" type="number" step="0.01" placeholder="0.00" />
                </label>

                <label>
                    <span>Description (Optional)</span>
                    <input v-model="description" type="text" placeholder="e.g., Dinner last night" maxlength="50" />
                </label>

                <button
                    class="btn primary-btn"
                    type="submit"
                    :disabled="submitting || !toIban"
                    style="margin-top: 1rem;">
                    {{ submitting ? 'Processing...' : 'Confirm Transfer' }}
                </button>
            </form>
        </section>
    </main>
</template>

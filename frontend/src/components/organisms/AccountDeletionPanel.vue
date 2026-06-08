<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { authorizedFetch } from '@/composables/useAuthorizedFetch';
import { parseApiErrorMessage } from '@/utils/apiError';
import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
    mode: 'self' | 'employee';
    targetUserId?: number;
    targetUserName?: string;
    deactivated?: boolean;
}>();

const emit = defineEmits<{ deleted: []; reactivated: [] }>();

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const error = ref<string | null>(null);
const success = ref<string | null>(null);
const confirmAction = ref<'deactivate' | 'permanent' | 'reactivate' | null>(null);

const targetLabel = () =>
    props.mode === 'self' ? 'your account' : `${props.targetUserName || 'this customer'}'s account`;

function openConfirm(action: 'deactivate' | 'permanent' | 'reactivate') {
    error.value = null;
    success.value = null;
    confirmAction.value = action;
}

function closeConfirm() {
    confirmAction.value = null;
}

async function confirmAndRun() {
    const action = confirmAction.value;
    if (!action) return;
    closeConfirm();

    if (action === 'reactivate') {
        await reactivateAccount();
        return;
    }

    await deleteAccount(action === 'permanent');
}

async function deleteAccount(permanent: boolean) {
    error.value = null;
    success.value = null;

    try {
        const url =
            props.mode === 'self'
                ? `/auth/me?permanent=${permanent}`
                : `/users/${props.targetUserId}?permanent=${permanent}`;

        const response = await authorizedFetch(url, { method: 'DELETE' });
        if (!response.ok) {
            const body = await response.text();
            throw new Error(parseApiErrorMessage(body, `Delete failed (${response.status})`));
        }

        if (props.mode === 'self') {
            auth.logout();
            router.push('/');
            return;
        }

        success.value = permanent ? 'Account permanently deleted.' : 'Account deactivated.';
        emit('deleted');
    } catch (err) {
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        loading.value = false;
    }
}

async function reactivateAccount() {
    if (props.mode !== 'employee' || !props.targetUserId) return;

    loading.value = true;
    error.value = null;
    success.value = null;

    try {
        const response = await authorizedFetch(`/users/${props.targetUserId}/reactivate`, {
            method: 'POST',
        });
        if (!response.ok) {
            const body = await response.text();
            throw new Error(parseApiErrorMessage(body, `Reactivate failed (${response.status})`));
        }

        success.value = 'Account reactivated.';
        emit('reactivated');
    } catch (err) {
        error.value = err instanceof Error ? err.message : String(err);
    } finally {
        loading.value = false;
    }
}
</script>

<template>
    <section class="limits-panel account-deletion-panel">
        <h3 style="margin: 0 0 0.75rem 0; font-size: 1rem;">Account deletion</h3>
        <p v-if="mode === 'self'" class="muted" style="margin: 0 0 1rem 0; font-size: 0.9rem;">
            Deactivate keeps your record in the system but blocks login and banking.
            Permanent delete requires zero balance on all accounts and frees your email for a new registration.
        </p>
        <p v-else class="muted" style="margin: 0 0 1rem 0; font-size: 0.9rem;">
            Deactivate hides the customer and blocks login. Permanent delete requires zero balance on all accounts.
        </p>

        <div class="button-group">
            <button
                v-if="mode === 'employee' && deactivated"
                class="btn primary-btn"
                type="button"
                :disabled="loading"
                @click="openConfirm('reactivate')">
                {{ loading ? 'Working...' : 'Reactivate account' }}
            </button>
            <button
                v-if="!deactivated"
                class="btn secondary-btn"
                type="button"
                :disabled="loading"
                @click="openConfirm('deactivate')">
                {{ loading ? 'Working...' : 'Deactivate account' }}
            </button>
            <button
                class="btn danger-btn"
                type="button"
                :disabled="loading"
                @click="openConfirm('permanent')">
                {{ loading ? 'Working...' : 'Permanently delete' }}
            </button>
        </div>

        <p v-if="error" class="error text-danger" style="margin-top: 0.75rem;">{{ error }}</p>
        <p v-if="success" style="color: #155724; font-weight: bold; margin-top: 0.75rem;">{{ success }}</p>
    </section>

    <div v-if="confirmAction" class="modal-overlay" @click.self="closeConfirm">
        <div class="panel modal-content">
            <h2 v-if="confirmAction === 'deactivate'">Deactivate account?</h2>
            <h2 v-else-if="confirmAction === 'permanent'">Permanently delete account?</h2>
            <h2 v-else>Reactivate account?</h2>

            <p v-if="confirmAction === 'deactivate'" class="muted">
                Are you sure you want to deactivate <strong>{{ targetLabel() }}</strong>?
                <span v-if="mode === 'self'"> You will be logged out and cannot sign in again.</span>
                <span v-else> They will be hidden from the directory and cannot log in. An employee can reactivate later.</span>
            </p>
            <p v-else-if="confirmAction === 'permanent'" class="muted">
                Are you sure you want to <strong>permanently delete</strong> {{ targetLabel() }}?
                All user data will be removed. This cannot be undone.
                All account balances must be zero.
            </p>
            <p v-else class="muted">
                Are you sure you want to reactivate <strong>{{ targetLabel() }}</strong>?
                They will be able to log in and use their accounts again.
            </p>

            <div class="button-group" style="margin-top: 1.25rem;">
                <button type="button" class="btn secondary-btn" @click="closeConfirm">Cancel</button>
                <button
                    type="button"
                    :class="confirmAction === 'permanent' ? 'btn danger-btn' : 'btn primary-btn'"
                    :disabled="loading"
                    @click="confirmAndRun">
                    {{
                        confirmAction === 'deactivate'
                            ? 'Yes, deactivate'
                            : confirmAction === 'permanent'
                              ? 'Yes, delete permanently'
                              : 'Yes, reactivate'
                    }}
                </button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    padding: 1rem;
}
.modal-content {
    width: 100%;
    max-width: 28rem;
}
</style>

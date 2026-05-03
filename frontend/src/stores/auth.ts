import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

export type LoginPayload = {
    email: string;
    password: string;
};

export type LoginResponse = {
    token: string;
    role: 'CUSTOMER' | 'EMPLOYEE';
    approved: boolean;
    // by Fernando
    firstName: string; 
};

const tokenStorageKey = 'code-generation-token';
const roleStorageKey = 'code-generation-role';
const approvedStorageKey = 'code-generation-approved';
// by Fernando: Define a storage key for the user's name
const nameStorageKey = 'code-generation-firstname'; 

export const useAuthStore = defineStore('auth', () => {
    const token = ref<string | null>(localStorage.getItem(tokenStorageKey));
    const role = ref<string | null>(localStorage.getItem(roleStorageKey));
    const approved = ref(localStorage.getItem(approvedStorageKey) === 'true');
    // by Fernando: Initialize firstName from localStorage
    const firstName = ref<string | null>(localStorage.getItem(nameStorageKey)); 
    
    const loading = ref(false);
    const error = ref<string | null>(null);

    const isAuthenticated = computed(() => Boolean(token.value));
    const isApprovedCustomer = computed(() => role.value === 'CUSTOMER' && approved.value);
    const isPendingCustomer = computed(() => role.value === 'CUSTOMER' && !approved.value);

    function setToken(value: string | null) {
        token.value = value;
        if (value) {
            localStorage.setItem(tokenStorageKey, value);
            return;
        }

        localStorage.removeItem(tokenStorageKey);
    }

    // by Fernando: Update setProfile to also handle the firstName
    function setProfile(loginRole: string | null, isApproved: boolean, userFirstName: string | null) {
        role.value = loginRole;
        approved.value = isApproved;
        firstName.value = userFirstName; // by Fernando

        if (loginRole) {
            localStorage.setItem(roleStorageKey, loginRole);
        } else {
            localStorage.removeItem(roleStorageKey);
        }
        
        localStorage.setItem(approvedStorageKey, String(isApproved));
        
        // by Fernando: Save or remove firstName from localStorage
        if (userFirstName) {
            localStorage.setItem(nameStorageKey, userFirstName);
        } else {
            localStorage.removeItem(nameStorageKey);
        }
    }

    async function login(payload: LoginPayload) {
        loading.value = true;
        error.value = null;

        try {
            const response = await fetch('/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || `Login failed (${response.status})`);
            }

            const data = (await response.json()) as LoginResponse;
            setToken(data.token);
            // by Fernando: Pass data.firstName into the setProfile function
            setProfile(data.role, data.approved, data.firstName); 
            return data;
        } catch (e) {
            error.value = e instanceof Error ? e.message : String(e);
            throw e;
        } finally {
            loading.value = false;
        }
    }

    function logout() {
        setToken(null);
        // by Fernando: Pass null for the firstName when logging out
        setProfile(null, false, null); 
    }

    return {
        token,
        role,
        approved,
        firstName, 
        loading,
        error,
        isAuthenticated,
        isApprovedCustomer,
        isPendingCustomer,
        login,
        logout,
    };
});
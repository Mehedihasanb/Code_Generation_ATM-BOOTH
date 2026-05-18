import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

export type LoginPayload = {
    email: string;
    password: string;
};

export type LoginResponse = {
    token: string;
    role: 'CUSTOMER' | 'EMPLOYEE';
    customerApprovalStatus?: string | null;
    employeeType?: 'REGULAR' | 'SERVICE_DESK'; //Added by Fernando
    firstName: string;
};

const tokenStorageKey = 'code-generation-token';
const roleStorageKey = 'code-generation-role';
const customerApprovalStatusStorageKey = 'code-generation-customer-approval-status';
const nameStorageKey = 'code-generation-firstname';
const employeeTypeStorageKey = 'code-generation-employee-type'; // Added by Fernando

function migrateLegacyApprovedFlag() {
    if (sessionStorage.getItem(customerApprovalStatusStorageKey) != null) {
        return;
    }
    const legacyApproved = sessionStorage.getItem('code-generation-approved');
    if (legacyApproved === 'true') {
        sessionStorage.setItem(customerApprovalStatusStorageKey, 'APPROVED');
    } else if (legacyApproved === 'false') {
        sessionStorage.setItem(customerApprovalStatusStorageKey, 'PENDING');
    }
    sessionStorage.removeItem('code-generation-approved');
}

migrateLegacyApprovedFlag();

export const useAuthStore = defineStore('auth', () => {
    // Upgraded to sessionStorage
    const token = ref<string | null>(sessionStorage.getItem(tokenStorageKey));
    const role = ref<string | null>(sessionStorage.getItem(roleStorageKey));
    const customerApprovalStatus = ref<string | null>(sessionStorage.getItem(customerApprovalStatusStorageKey));
    const firstName = ref<string | null>(sessionStorage.getItem(nameStorageKey));
    const employeeType = ref<string | null>(sessionStorage.getItem(employeeTypeStorageKey)); // Added by Fernando

    const loading = ref(false);
    const error = ref<string | null>(null);

    const isAuthenticated = computed(() => Boolean(token.value));
    const isApprovedCustomer = computed(
        () => role.value === 'CUSTOMER' && customerApprovalStatus.value === 'APPROVED'
    );
    const isPendingCustomer = computed(
        () => role.value === 'CUSTOMER' && customerApprovalStatus.value === 'PENDING'
    );
    const isDeniedCustomer = computed(
        () => role.value === 'CUSTOMER' && customerApprovalStatus.value === 'DENIED'
    );

    function setToken(newTokenValue: string | null) {
        token.value = newTokenValue;
        if (newTokenValue) {
            sessionStorage.setItem(tokenStorageKey, newTokenValue);
            return;
        }
        sessionStorage.removeItem(tokenStorageKey);
    }

    function setProfile(
        loginRole: string | null,
        status: string | null,
        userFirstName: string | null,
        type: string | null // Added by Fernando
    ) {
        role.value = loginRole;
        customerApprovalStatus.value = status;
        firstName.value = userFirstName;
        employeeType.value = type; // Added by Fernando -->
        if (type) {
            sessionStorage.setItem(employeeTypeStorageKey, type);
        } else {
            sessionStorage.removeItem(employeeTypeStorageKey);
        } // Added by Fernando <--

        if (loginRole) {
            sessionStorage.setItem(roleStorageKey, loginRole);
        } else {
            sessionStorage.removeItem(roleStorageKey);
        }

        if (status != null && status !== '') {
            sessionStorage.setItem(customerApprovalStatusStorageKey, status);
        } else {
            sessionStorage.removeItem(customerApprovalStatusStorageKey);
        }

        if (userFirstName) {
            sessionStorage.setItem(nameStorageKey, userFirstName);
        } else {
            sessionStorage.removeItem(nameStorageKey);
        }
    }

    async function login(loginPayload: LoginPayload) {
        loading.value = true;
        error.value = null;

        try {
            const loginHttpResponse = await fetch('/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(loginPayload),
            });

            if (!loginHttpResponse.ok) {
                const errorResponseText = await loginHttpResponse.text();
                throw new Error(
                    errorResponseText || `Login failed (${loginHttpResponse.status})`
                );
            }

            const loginResponseBody = (await loginHttpResponse.json()) as LoginResponse;
            setToken(loginResponseBody.token);
            const status =
                loginResponseBody.customerApprovalStatus != null &&
                loginResponseBody.customerApprovalStatus !== ''
                    ? loginResponseBody.customerApprovalStatus
                    : null;
            setProfile(loginResponseBody.role, status, loginResponseBody.firstName, loginResponseBody.employeeType ?? null);
            return loginResponseBody;
        } catch (loginFailure) {
            error.value = loginFailure instanceof Error ? loginFailure.message : String(loginFailure);
            throw loginFailure;
        } finally {
            loading.value = false;
        }
    }

    function logout() {
        setToken(null);
        setProfile(null, null, null, null);
    }

    return {
        token,
        employeeType, // Added by Fernando
        role,
        customerApprovalStatus,
        firstName,
        loading,
        error,
        isAuthenticated,
        isApprovedCustomer,
        isPendingCustomer,
        isDeniedCustomer,
        login,
        logout,
    };
});
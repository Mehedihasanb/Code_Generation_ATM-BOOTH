import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

export type LoginPayload = {
	email: string;
	password: string;
};

export type LoginResponse = {
	token: string;
	role: 'CUSTOMER' | 'EMPLOYEE';
	/** PENDING, APPROVED, or DENIED for customers; omitted or null for employees. */
	customerApprovalStatus?: string | null;
	firstName: string;
};

const tokenStorageKey = 'code-generation-token';
const roleStorageKey = 'code-generation-role';
const customerApprovalStatusStorageKey = 'code-generation-customer-approval-status';
const nameStorageKey = 'code-generation-firstname';

function migrateLegacyApprovedFlag() {
	if (localStorage.getItem(customerApprovalStatusStorageKey) != null) {
		return;
	}
	const legacyApproved = localStorage.getItem('code-generation-approved');
	if (legacyApproved === 'true') {
		localStorage.setItem(customerApprovalStatusStorageKey, 'APPROVED');
	} else if (legacyApproved === 'false') {
		localStorage.setItem(customerApprovalStatusStorageKey, 'PENDING');
	}
	localStorage.removeItem('code-generation-approved');
}

migrateLegacyApprovedFlag();

export const useAuthStore = defineStore('auth', () => {
	const token = ref<string | null>(localStorage.getItem(tokenStorageKey));
	const role = ref<string | null>(localStorage.getItem(roleStorageKey));
	const customerApprovalStatus = ref<string | null>(localStorage.getItem(customerApprovalStatusStorageKey));
	const firstName = ref<string | null>(localStorage.getItem(nameStorageKey));

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
			localStorage.setItem(tokenStorageKey, newTokenValue);
			return;
		}
		localStorage.removeItem(tokenStorageKey);
	}

	function setProfile(
		loginRole: string | null,
		status: string | null,
		userFirstName: string | null
	) {
		role.value = loginRole;
		customerApprovalStatus.value = status;
		firstName.value = userFirstName;

		if (loginRole) {
			localStorage.setItem(roleStorageKey, loginRole);
		} else {
			localStorage.removeItem(roleStorageKey);
		}

		if (status != null && status !== '') {
			localStorage.setItem(customerApprovalStatusStorageKey, status);
		} else {
			localStorage.removeItem(customerApprovalStatusStorageKey);
		}

		if (userFirstName) {
			localStorage.setItem(nameStorageKey, userFirstName);
		} else {
			localStorage.removeItem(nameStorageKey);
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
			setProfile(loginResponseBody.role, status, loginResponseBody.firstName);
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
		setProfile(null, null, null);
	}

	return {
		token,
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

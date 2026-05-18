import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

export type LoginPayload = {
	email: string;
	password: string;
};

type LoginResponse = {
	token: string;
	role: 'CUSTOMER' | 'EMPLOYEE';
	customerApprovalStatus?: string | null;
	employeeType?: string | null;
	firstName: string;
};

// router guard reads these from localStorage
export const tokenStorageKey = 'code-generation-token';
export const roleStorageKey = 'code-generation-role';
export const customerApprovalStatusStorageKey = 'code-generation-customer-approval-status';
const employeeTypeStorageKey = 'code-generation-employee-type';
const nameStorageKey = 'code-generation-firstname';

if (localStorage.getItem(customerApprovalStatusStorageKey) == null) {
	const legacyApproved = localStorage.getItem('code-generation-approved');
	if (legacyApproved === 'true') {
		localStorage.setItem(customerApprovalStatusStorageKey, 'APPROVED');
	} else if (legacyApproved === 'false') {
		localStorage.setItem(customerApprovalStatusStorageKey, 'PENDING');
	}
	localStorage.removeItem('code-generation-approved');
}

export const useAuthStore = defineStore('auth', () => {
	const token = ref<string | null>(localStorage.getItem(tokenStorageKey));
	const role = ref<string | null>(localStorage.getItem(roleStorageKey));
	const customerApprovalStatus = ref<string | null>(
		localStorage.getItem(customerApprovalStatusStorageKey)
	);
	const employeeType = ref<string | null>(localStorage.getItem(employeeTypeStorageKey));
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

	function setToken(newToken: string | null) {
		token.value = newToken;
		if (newToken) {
			localStorage.setItem(tokenStorageKey, newToken);
		} else {
			localStorage.removeItem(tokenStorageKey);
		}
	}

	function setProfile(
		loginRole: string | null,
		status: string | null,
		userFirstName: string | null,
		userEmployeeType: string | null
	) {
		role.value = loginRole;
		customerApprovalStatus.value = status;
		firstName.value = userFirstName;
		employeeType.value = userEmployeeType;

		if (loginRole) {
			localStorage.setItem(roleStorageKey, loginRole);
		} else {
			localStorage.removeItem(roleStorageKey);
		}

		if (status) {
			localStorage.setItem(customerApprovalStatusStorageKey, status);
		} else {
			localStorage.removeItem(customerApprovalStatusStorageKey);
		}

		if (userEmployeeType) {
			localStorage.setItem(employeeTypeStorageKey, userEmployeeType);
		} else {
			localStorage.removeItem(employeeTypeStorageKey);
		}

		if (userFirstName) {
			localStorage.setItem(nameStorageKey, userFirstName);
		} else {
			localStorage.removeItem(nameStorageKey);
		}
	}

	async function login(loginRequest: LoginPayload) {
		loading.value = true;
		error.value = null;

		try {
			// Call backend (same as AuthController -> RegistrationService.login)
			const loginHttpResponse = await fetch('/auth/login', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(loginRequest),
			});

			if (!loginHttpResponse.ok) {
				const errorText = await loginHttpResponse.text();
				throw new Error(errorText || `Login failed (${loginHttpResponse.status})`);
			}

			const loginResponseBody = (await loginHttpResponse.json()) as LoginResponse;

			// Save token + profile for the rest of the app
			setToken(loginResponseBody.token);

			let approvalStatus: string | null = null;
			if (loginResponseBody.customerApprovalStatus != null
				&& loginResponseBody.customerApprovalStatus !== '') {
				approvalStatus = loginResponseBody.customerApprovalStatus;
			}

			setProfile(
				loginResponseBody.role,
				approvalStatus,
				loginResponseBody.firstName,
				loginResponseBody.employeeType ?? null
			);

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
		role,
		customerApprovalStatus,
		employeeType,
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

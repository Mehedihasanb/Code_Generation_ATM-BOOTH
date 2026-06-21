import { defineStore } from 'pinia';
import { ref } from 'vue';
import { apiUrl } from '@/config/api';
import { parseApiErrorMessage } from '@/utils/apiError';

export type RegisterPayload = {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
	bsnNumber: string;
	phoneNumber: string;
};

type RegisterResponse = {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	role: string;
};

export const useRegistrationStore = defineStore('registration', () => {
	const loading = ref(false);
	const error = ref<string | null>(null);
	const success = ref<string | null>(null);

	async function submitRegistration(registerRequest: RegisterPayload) {
		loading.value = true;
		error.value = null;
		success.value = null;

		try {
			// Call backend (same as AuthController -> RegistrationService.register)
			const registerHttpResponse = await fetch(apiUrl('/auth/register'), {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					firstName: registerRequest.firstName,
					lastName: registerRequest.lastName,
					email: registerRequest.email,
					password: registerRequest.password,
					bsn: registerRequest.bsnNumber,
					phoneNumber: registerRequest.phoneNumber,
				}),
			});

			if (!registerHttpResponse.ok) {
				const errorText = await registerHttpResponse.text();
				throw new Error(
					parseApiErrorMessage(
						errorText,
						`Registration failed (${registerHttpResponse.status})`
					)
				);
			}

			const registerResponseBody = (await registerHttpResponse.json()) as RegisterResponse;
			success.value = `Registration submitted for ${registerResponseBody.email}. Please wait for employee approval.`;
			return registerResponseBody;
		} catch (registrationFailure) {
			error.value = registrationFailure instanceof Error
				? registrationFailure.message
				: String(registrationFailure);
			throw registrationFailure;
		} finally {
			loading.value = false;
		}
	}

	return { loading, error, success, submitRegistration };
});

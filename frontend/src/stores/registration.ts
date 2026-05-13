import { defineStore } from 'pinia';
import { ref } from 'vue';

export type RegisterPayload = {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
    bsnNumber: string;
    phoneNumber: string;
};

export type RegisterResponse = {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	message: string;
};

export const useRegistrationStore = defineStore('registration', () => {
	const loading = ref(false);
	const error = ref<string | null>(null);
	const success = ref<string | null>(null);

	async function submitRegistration(registrationPayload: RegisterPayload) {
		loading.value = true;
		error.value = null;
		success.value = null;
		try {
			const registrationHttpResponse = await fetch('/auth/register', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify(registrationPayload),
			});

			if (!registrationHttpResponse.ok) {
				const errorResponseText = await registrationHttpResponse.text();
				throw new Error(errorResponseText || `Registration failed (${registrationHttpResponse.status})`);
			}

			const registrationResponseBody = (await registrationHttpResponse.json()) as RegisterResponse;
			success.value = registrationResponseBody.message;
			return registrationResponseBody;
		} catch (registrationFailure) {
			error.value = registrationFailure instanceof Error ? registrationFailure.message : String(registrationFailure);
			throw registrationFailure;
		} finally {
			loading.value = false;
		}
	}

	return { loading, error, success, submitRegistration };
});
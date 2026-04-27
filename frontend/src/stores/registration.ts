import { defineStore } from 'pinia';
import { ref } from 'vue';

export type RegisterPayload = {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
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

	async function submitRegistration(payload: RegisterPayload) {
		loading.value = true;
		error.value = null;
		success.value = null;
		try {
			const response = await fetch('/api/registrations', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify(payload),
			});

			if (!response.ok) {
				const text = await response.text();
				throw new Error(text || `Registration failed (${response.status})`);
			}

			const data = (await response.json()) as RegisterResponse;
			success.value = data.message;
			return data;
		} catch (e) {
			error.value = e instanceof Error ? e.message : String(e);
			throw e;
		} finally {
			loading.value = false;
		}
	}

	return { loading, error, success, submitRegistration };
});
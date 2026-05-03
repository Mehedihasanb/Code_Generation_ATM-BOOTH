import { defineStore } from 'pinia';
import { ref } from 'vue';
import { useAuthStore } from './auth';

export type HealthResponse = { status: string };
export type MessageDto = { id: number; text: string };

export const useApiStore = defineStore('api', () => {
	const health = ref<HealthResponse | null>(null);
	const messages = ref<MessageDto[]>([]);
	const loading = ref(false);
	const error = ref<string | null>(null);

	async function fetchFromBackend() {
		loading.value = true;
		error.value = null;
		const auth = useAuthStore();
		const headers: HeadersInit = {};

		if (auth.token) {
			headers.Authorization = `Bearer ${auth.token}`;
		}
		try {
			const healthRes = await fetch('/api/health');
			if (!healthRes.ok) {
				throw new Error(`Health: ${healthRes.status}`);
			}
			health.value = (await healthRes.json()) as HealthResponse;

			const messagesRes = await fetch('/api/messages', { headers });
			if (!messagesRes.ok) {
				throw new Error(messagesRes.status === 401 ? 'Please log in to view messages.' : `Messages: ${messagesRes.status}`);
			}
			messages.value = (await messagesRes.json()) as MessageDto[];
		} catch (e) {
			error.value = e instanceof Error ? e.message : String(e);
			health.value = null;
			messages.value = [];
		} finally {
			loading.value = false;
		}
	}

	return { health, messages, loading, error, fetchFromBackend };
});

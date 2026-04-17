import { defineStore } from 'pinia';
import { ref } from 'vue';

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
		try {
			const healthRes = await fetch('/api/health');
			if (!healthRes.ok) {
				throw new Error(`Health: ${healthRes.status}`);
			}
			health.value = (await healthRes.json()) as HealthResponse;

			const messagesRes = await fetch('/api/messages');
			if (!messagesRes.ok) {
				throw new Error(`Messages: ${messagesRes.status}`);
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

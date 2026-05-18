import { defineStore } from 'pinia';
import { ref } from 'vue';

export type HealthResponse = {
	status: string;
};

export const useApiStore = defineStore('api', () => {
	const health = ref<HealthResponse | null>(null);
	const loading = ref(false);
	const error = ref<string | null>(null);

	async function fetchFromBackend() {
		loading.value = true;
		error.value = null;

		try {
			// Public health check (no auth)
			const healthHttpResponse = await fetch('/api/health');

			if (!healthHttpResponse.ok) {
				throw new Error(`Health check failed (${healthHttpResponse.status})`);
			}

			health.value = (await healthHttpResponse.json()) as HealthResponse;
		} catch (healthFailure) {
			error.value = healthFailure instanceof Error ? healthFailure.message : String(healthFailure);
			health.value = null;
		} finally {
			loading.value = false;
		}
	}

	return { health, loading, error, fetchFromBackend };
});

import { defineStore } from 'pinia';

import { ref } from 'vue';



export type HealthResponse = { status: string };



export const useApiStore = defineStore('api', () => {

	const health = ref<HealthResponse | null>(null);

	const loading = ref(false);

	const error = ref<string | null>(null);



	async function fetchFromBackend() {

		loading.value = true;

		error.value = null;

		try {

			const healthCheckResponse = await fetch('/api/health');

			if (!healthCheckResponse.ok) {

				throw new Error(`Health: ${healthCheckResponse.status}`);

			}

			health.value = (await healthCheckResponse.json()) as HealthResponse;

		} catch (backendFetchFailure) {

			error.value = backendFetchFailure instanceof Error ? backendFetchFailure.message : String(backendFetchFailure);

			health.value = null;

		} finally {

			loading.value = false;

		}

	}



	return { health, loading, error, fetchFromBackend };

});


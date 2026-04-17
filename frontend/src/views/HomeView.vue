<script setup lang="ts">
import { onMounted } from 'vue';
import { useApiStore } from '../stores/api';

const api = useApiStore();

onMounted(() => {
	void api.fetchFromBackend();
});
</script>

<template>
	<section class="panel">
		<h1>Backend connection</h1>
		<p class="hint">
			The dev server proxies <code>/api</code> to <code>http://localhost:8080</code>. Start the Spring Boot app,
			then reload this page.
		</p>

		<p v-if="api.loading" class="status">Loading…</p>
		<p v-else-if="api.error" class="error">{{ api.error }}</p>
		<div v-else class="results">
			<p>
				<strong>Health:</strong>
				{{ api.health ? JSON.stringify(api.health) : '—' }}
			</p>
			<p><strong>Messages (JPA):</strong></p>
			<ul v-if="api.messages.length">
				<li v-for="m in api.messages" :key="m.id">{{ m.text }}</li>
			</ul>
			<p v-else class="muted">No messages returned.</p>
		</div>

		<button type="button" class="btn" :disabled="api.loading" @click="api.fetchFromBackend()">Retry</button>
	</section>
</template>

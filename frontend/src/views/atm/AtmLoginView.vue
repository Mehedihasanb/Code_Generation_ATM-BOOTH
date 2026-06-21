<script setup lang="ts">
// ATM login — submits credentials to the backend; access rules are enforced server-side.
import { reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

type LoginForm = {
	email: string;
	password: string;
};

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const form = reactive<LoginForm>({
	email: '',
	password: '',
});

async function submit() {
	try {
		await auth.login({
			email: form.email.trim(),
			password: form.password,
		});

		const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/atm/home';
		await router.push(redirect);
	} catch {
		// auth.error is set in the store from the backend response
	}
}
</script>

<template>
	<div class="atm-shell">
		<section class="atm-panel atm-login-panel">
			<header class="atm-home-header">
				<div>
					<p class="atm-session-label">Customer ATM</p>
					<h1>RhinoBank ATM</h1>
				</div>
				<button type="button" class="atm-btn atm-btn-secondary" @click="router.push('/')">
					&larr; Back
				</button>
			</header>

			<div class="atm-brand">
				<span class="atm-logo" aria-hidden="true">ATM</span>
				<p class="atm-tagline">Sign in with your online banking credentials</p>
			</div>

			<form class="atm-form" @submit.prevent="submit">
				<label>
					<span>Email</span>
					<input
						v-model="form.email"
						type="email"
						autocomplete="email"
						placeholder="customer@inholland.nl" />
				</label>

				<label>
					<span>Password</span>
					<input
						v-model="form.password"
						type="password"
						autocomplete="current-password"
						placeholder="••••••••" />
				</label>

				<p v-if="auth.error" class="error atm-error">{{ auth.error }}</p>

				<button class="atm-btn atm-btn-primary" type="submit" :disabled="auth.loading">
					{{ auth.loading ? 'Signing in...' : 'Enter ATM' }}
				</button>
			</form>
		</section>
	</div>
</template>

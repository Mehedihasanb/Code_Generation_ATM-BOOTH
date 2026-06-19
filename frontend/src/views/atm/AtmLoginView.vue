<script setup lang="ts">
// ATM login screen. Reuses /auth/login, then checks customer is approved.
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

const fieldError = reactive<Record<keyof LoginForm, string>>({
	email: '',
	password: '',
});

function clearFieldErrors() {
	fieldError.email = '';
	fieldError.password = '';
}

function validate(): boolean {
	clearFieldErrors();
	let valid = true;

	if (!form.email.trim()) {
		fieldError.email = 'Email is required.';
		valid = false;
	} else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
		fieldError.email = 'Email format is invalid.';
		valid = false;
	}

	if (!form.password) {
		fieldError.password = 'Password is required.';
		valid = false;
	}

	return valid;
}

async function submit() {
	if (!validate()) {
		return;
	}

	try {
		// Step 1: same login API as online banking — returns JWT stored in sessionStorage
		await auth.login({
			email: form.email.trim(),
			password: form.password,
		});

		// Step 2: ATM-specific rules — only approved customers may continue
		if (auth.role === 'EMPLOYEE') {
			auth.logout();
			auth.error = 'ATM login is for customers only. Please use the employee portal.';
			return;
		}

		if (auth.isPendingCustomer) {
			auth.logout();
			auth.error = 'Your account is still pending approval. You cannot use the ATM yet.';
			return;
		}

		if (auth.isDeniedCustomer) {
			auth.logout();
			auth.error = 'Your account has been denied. You cannot use the ATM.';
			return;
		}

		if (!auth.isApprovedCustomer) {
			auth.logout();
			auth.error = 'Only approved customers can use the ATM.';
			return;
		}

		// Step 3: show ATM home screen (balances + withdraw/deposit menu)
		const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/atm/home';
		await router.push(redirect);
	} catch {
		// auth.error is set in the store
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
					<small v-if="fieldError.email" class="error">{{ fieldError.email }}</small>
				</label>

				<label>
					<span>Password</span>
					<input
						v-model="form.password"
						type="password"
						autocomplete="current-password"
						placeholder="••••••••" />
					<small v-if="fieldError.password" class="error">{{ fieldError.password }}</small>
				</label>

				<p v-if="auth.error" class="error atm-error">{{ auth.error }}</p>

				<button class="atm-btn atm-btn-primary" type="submit" :disabled="auth.loading">
					{{ auth.loading ? 'Signing in...' : 'Enter ATM' }}
				</button>
			</form>
		</section>
	</div>
</template>

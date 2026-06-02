<script setup lang="ts">
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
		await auth.login({
			email: form.email.trim(),
			password: form.password,
		});

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
			<div class="atm-brand">
				<span class="atm-logo" aria-hidden="true">🏧</span>
				<h1>RhinoBank ATM</h1>
				<p class="atm-tagline">Insert card — sign in with your online banking credentials</p>
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

			<p class="atm-footnote muted">Secured with JWT — same login as online banking.</p>
		</section>
	</div>
</template>

<style scoped>
.atm-shell {
	min-height: calc(100vh - 2rem);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 1.5rem;
	background: linear-gradient(160deg, #0f172a 0%, #1e3a5f 45%, #0f172a 100%);
}

.atm-login-panel {
	width: 100%;
	max-width: 420px;
	background: #1e293b;
	border: 2px solid #334155;
	border-radius: 16px;
	padding: 2rem;
	color: #f8fafc;
	box-shadow: 0 24px 48px rgba(0, 0, 0, 0.35);
}

.atm-brand {
	text-align: center;
	margin-bottom: 1.75rem;
}

.atm-logo {
	font-size: 2.5rem;
	display: block;
	margin-bottom: 0.5rem;
}

.atm-brand h1 {
	margin: 0 0 0.5rem;
	font-size: 1.5rem;
	color: #f8fafc;
}

.atm-tagline {
	margin: 0;
	font-size: 0.9rem;
	color: #94a3b8;
}

.atm-form {
	display: grid;
	gap: 1rem;
}

.atm-form label {
	display: grid;
	gap: 0.35rem;
	font-weight: 600;
	font-size: 0.9rem;
	color: #cbd5e1;
}

.atm-form input {
	padding: 0.75rem;
	border: 1px solid #475569;
	border-radius: 8px;
	background: #0f172a;
	color: #f8fafc;
	font: inherit;
}

.atm-form input:focus {
	outline: none;
	border-color: #38bdf8;
	box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.25);
}

.atm-btn {
	padding: 0.85rem 1.25rem;
	border-radius: 10px;
	border: none;
	font: inherit;
	font-weight: 700;
	cursor: pointer;
	width: 100%;
	margin-top: 0.25rem;
}

.atm-btn-primary {
	background: linear-gradient(180deg, #22c55e 0%, #16a34a 100%);
	color: #052e16;
}

.atm-btn-primary:hover:not(:disabled) {
	filter: brightness(1.05);
}

.atm-btn-primary:disabled {
	opacity: 0.6;
	cursor: not-allowed;
}

.atm-error {
	margin: 0;
	text-align: center;
}

.atm-footnote {
	margin: 1.25rem 0 0;
	text-align: center;
	font-size: 0.8rem;
	color: #64748b;
}
</style>

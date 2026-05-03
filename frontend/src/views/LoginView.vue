<script setup lang="ts">
import { reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

type LoginForm = {
	email: string;
	password: string;
};

const auth = useAuthStore();
const router = useRouter();

const form = reactive<LoginForm>({
	email: '',
	password: '',
});

const fieldError = reactive<Record<keyof LoginForm, string>>({
	email: '',
	password: '',
});

const isLoggedIn = computed(() => auth.isAuthenticated);

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

	await auth.login({
		email: form.email.trim(),
		password: form.password,
	});

	if (auth.isPendingCustomer) {
		await router.push('/pending-approval');
		return;
	}

	await router.push('/');
}
</script>

<template>
	<section class="panel auth-panel">
		<h1>Login</h1>
		<p class="hint">Authenticate through the backend REST API and receive a JWT.</p>

		<form class="auth-form" @submit.prevent="submit">
			<label>
				<span>Email</span>
				<input v-model="form.email" type="email" autocomplete="email" />
				<small v-if="fieldError.email" class="error">{{ fieldError.email }}</small>
			</label>

			<label>
				<span>Password</span>
				<input v-model="form.password" type="password" autocomplete="current-password" />
				<small v-if="fieldError.password" class="error">{{ fieldError.password }}</small>
			</label>

			<p v-if="auth.error" class="error">{{ auth.error }}</p>
			<p v-else-if="isLoggedIn" class="success">You are logged in.</p>

			<button class="btn" type="submit" :disabled="auth.loading">
				{{ auth.loading ? 'Signing in...' : 'Login' }}
			</button>
		</form>
	</section>
</template>

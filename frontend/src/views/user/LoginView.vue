<script setup lang="ts">
import { reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';

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

const isLoggedIn = computed(() => auth.isAuthenticated);

function clearLoginError() {
    auth.error = null;
}

onMounted(() => {
    clearLoginError();
});

async function submit() {
    clearLoginError();

    try {
        await auth.login({
            email: form.email.trim(),
            password: form.password,
        });

        const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null;
        if (redirect) {
            await router.push(redirect);
            return;
        }

        if (auth.role === 'EMPLOYEE') {
            await router.push('/service-desk');
            return;
        }

        await router.push('/');
        
    } catch {
    }
}
</script>

<template>
	<section class="panel auth-panel">
		<h1>Login</h1>
		<p class="hint">Sign in to your account.</p>

		<form class="auth-form" @submit.prevent="submit">
			<label>
				<span>Email</span>
				<input v-model="form.email" type="email" autocomplete="email" @input="clearLoginError" />
			</label>

			<label>
				<span>Password</span>
				<input v-model="form.password" type="password" autocomplete="current-password" @input="clearLoginError" />
			</label>

			<p v-if="auth.error" class="error">{{ auth.error }}</p>
			<p v-else-if="isLoggedIn" class="success">Welcome back!</p>

			<button class="btn primary-btn" type="submit" :disabled="auth.loading">
				{{ auth.loading ? 'Signing in...' : 'Login' }}
			</button>
		</form>
	</section>
</template>
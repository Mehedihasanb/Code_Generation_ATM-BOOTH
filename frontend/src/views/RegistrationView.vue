<script setup lang="ts">
import { reactive } from 'vue';
import { useRegistrationStore } from '../stores/registration';

type RegistrationForm = {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
    bsnNumber: string;
    phoneNumber: string;
};

const registration = useRegistrationStore();

const form = reactive<RegistrationForm>({
	firstName: '',
	lastName: '',
	email: '',
	password: '',
    bsnNumber: '',
    phoneNumber: '',
});

const fieldError = reactive<Record<keyof RegistrationForm, string>>({
	firstName: '',
	lastName: '',
	email: '',
	password: '',
    bsnNumber: '',
    phoneNumber: '',
});

function clearFieldErrors() {
	fieldError.firstName = '';
	fieldError.lastName = '';
	fieldError.email = '';
	fieldError.password = '';
}

function validate(): boolean {
	clearFieldErrors();
	let valid = true;

	if (!form.firstName.trim()) {
		fieldError.firstName = 'First name is required.';
		valid = false;
	}

	if (!form.lastName.trim()) {
		fieldError.lastName = 'Last name is required.';
		valid = false;
	}

	if (!form.email.trim()) {
		fieldError.email = 'Email is required.';
		valid = false;
	} else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
		fieldError.email = 'Email format is invalid.';
		valid = false;
	}

	if (form.password.length < 8) {
		fieldError.password = 'Password must be at least 8 characters.';
		valid = false;
	}

	if (!form.bsnNumber.trim()) {
		fieldError.bsnNumber = 'BSN number is required.';
		valid = false;
	}

	if (!form.phoneNumber.trim()) {
		fieldError.phoneNumber = 'Phone number is required.';
		valid = false;
	}

	return valid;
}

async function submit() {
	if (!validate()) {
		return;
	}

	await registration.submitRegistration({
		firstName: form.firstName.trim(),
		lastName: form.lastName.trim(),
		email: form.email.trim(),
		password: form.password,
		bsnNumber: form.bsnNumber.trim(),
		phoneNumber: form.phoneNumber.trim(),
	});

	form.firstName = '';
	form.lastName = '';
	form.email = '';
	form.password = '';
	clearFieldErrors();
}
</script>

<template>
	<section class="panel register-panel">
		<h1>Create Account</h1>
		<p class="hint">Register through the backend REST API.</p>

		<form class="register-form" @submit.prevent="submit">
			<label>
				<span>First Name</span>
				<input v-model="form.firstName" type="text" autocomplete="given-name" />
				<small v-if="fieldError.firstName" class="error">{{ fieldError.firstName }}</small>
			</label>

			<label>
				<span>Last Name</span>
				<input v-model="form.lastName" type="text" autocomplete="family-name" />
				<small v-if="fieldError.lastName" class="error">{{ fieldError.lastName }}</small>
			</label>

			<label>
				<span>Email</span>
				<input v-model="form.email" type="email" autocomplete="email" />
				<small v-if="fieldError.email" class="error">{{ fieldError.email }}</small>
			</label>

			<label>
				<span>Password</span>
				<input v-model="form.password" type="password" autocomplete="new-password" />
				<small v-if="fieldError.password" class="error">{{ fieldError.password }}</small>
			</label>

			<label>
				<span>BSN Number</span>
				<input v-model="form.bsnNumber" type="text" autocomplete="bsn" />
				<small v-if="fieldError.bsnNumber" class="error">{{ fieldError.bsnNumber }}</small>
			</label>

			<label>
				<span>Phone Number</span>
				<input v-model="form.phoneNumber" type="tel" autocomplete="tel" />
				<small v-if="fieldError.phoneNumber" class="error">{{ fieldError.phoneNumber }}</small>
			</label>

			<p v-if="registration.success" class="success">{{ registration.success }}</p>
			<p v-if="registration.error" class="error">{{ registration.error }}</p>

			<button class="btn" type="submit" :disabled="registration.loading">
				{{ registration.loading ? 'Submitting...' : 'Register' }}
			</button>
		</form>
	</section>
</template>
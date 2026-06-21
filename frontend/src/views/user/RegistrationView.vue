<script setup lang="ts">
import { reactive, onUnmounted } from 'vue';
import { useRegistrationStore } from '../../stores/registration';
import { useRouter } from 'vue-router';

type RegistrationForm = {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    bsnNumber: string;
    phoneNumber: string;
};

const router = useRouter();
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
    fieldError.bsnNumber = '';
    fieldError.phoneNumber = '';
}

function applyBackendFieldErrors(errors: Record<string, string>) {
    clearFieldErrors();
    const fieldMap: Record<string, keyof RegistrationForm> = {
        firstName: 'firstName',
        lastName: 'lastName',
        email: 'email',
        password: 'password',
        bsn: 'bsnNumber',
        phoneNumber: 'phoneNumber',
    };
    for (const [key, message] of Object.entries(errors)) {
        const formField = fieldMap[key];
        if (formField) {
            fieldError[formField] = message;
        }
    }
}

async function submit() {
    clearFieldErrors();

    try {
        await registration.submitRegistration({
            firstName: form.firstName.trim(),
            lastName: form.lastName.trim(),
            email: form.email.trim(),
            password: form.password,
            bsnNumber: form.bsnNumber.trim(),
            phoneNumber: form.phoneNumber.trim(),
        });

        applyBackendFieldErrors(registration.fieldErrors);

        if (!registration.error) {
            
            form.firstName = '';
            form.lastName = '';
            form.email = '';
            form.password = '';
            form.bsnNumber = '';
            form.phoneNumber = '';
            clearFieldErrors();

            setTimeout(() => {
                router.push('/login');
            }, 1500);
        }
    } catch {
        applyBackendFieldErrors(registration.fieldErrors);
        if (!registration.error) {
            registration.error =
                'Something went wrong while connecting to the server. Please try again later.';
        }
    }
}
onUnmounted(() => {
    registration.success = ''; 
    registration.error = '';
});

</script>

<template>
    <section class="panel register-panel">
        <h1>Create Account</h1>
        <p class="hint">Create your account to get started.</p>

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

            <button class="btn primary-btn" type="submit" :disabled="registration.loading">
                {{ registration.loading ? 'Submitting...' : 'Register' }}
            </button>
        </form>
    </section>
</template>
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

    if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*[^a-zA-Z0-9]).{8,}$/.test(form.password)) {
        fieldError.password = 'Password needs 8+ chars with upper, lower, and a special character.';
        valid = false;
    }

    if (!form.bsnNumber.trim()) {
        fieldError.bsnNumber = 'BSN number is required.';
        valid = false;
    } else if (!/^\d{8,9}$/.test(form.bsnNumber.trim())) {
        fieldError.bsnNumber = 'BSN must be 8 or 9 digits.';
        valid = false;
    }

    if (!form.phoneNumber.trim()) {
        fieldError.phoneNumber = 'Phone number is required.';
        valid = false;
    } else if (!/^06[0-9]{8}$/.test(form.phoneNumber.trim())) {
        fieldError.phoneNumber = 'Phone must start with 06 followed by 8 digits.';
        valid = false;
    }

    return valid;
}

async function submit() {
    if (!validate()) {
        return;
    }

    try {
        await registration.submitRegistration({
            firstName: form.firstName.trim(),
            lastName: form.lastName.trim(),
            email: form.email.trim(),
            password: form.password,
            bsnNumber: form.bsnNumber.trim(),
            phoneNumber: form.phoneNumber.trim(),
        });

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
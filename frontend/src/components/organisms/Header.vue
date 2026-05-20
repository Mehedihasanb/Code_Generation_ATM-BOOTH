<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth'; 

const auth = useAuthStore();
const router = useRouter();

const handleLogout = () => {
    auth.logout();
    router.push('/');
};
</script>

<template>
    <header class="header">
        <strong class="brand">RhinoBank</strong>
        
        <nav class="nav">
            <RouterLink to="/">Home</RouterLink>

            <RouterLink 
                v-if="auth.isEmployee" 
                to="/service-desk" 
                class="special-nav-link"
            >
                Service Desk
            </RouterLink>

            <RouterLink 
                v-if="auth.isApprovedCustomer" 
                to="/accounts" 
            >
                My Accounts
            </RouterLink>

            <RouterLink v-if="auth.isApprovedCustomer" to="/transfer">
                Transfer Funds
            </RouterLink>

            <template v-if="auth.isAuthenticated">
                <span class="greeting">Hello, {{ auth.firstName || 'User' }}</span>
                <button @click="handleLogout" class="btn-link">Logout</button>
            </template>

            <template v-else>
                <RouterLink to="/login">Login</RouterLink>
                <RouterLink to="/register">Register</RouterLink>
            </template>
        </nav>
    </header>
</template>

<style scoped>
.special-nav-link {
    font-weight: 600;
    color: #d35400; 
}
</style>
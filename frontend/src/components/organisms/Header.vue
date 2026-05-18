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
                v-if="auth.role === 'EMPLOYEE'" 
                to="/service-desk" 
                class="special-nav-link"
            >
                Service Desk
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
//Optional: Give the employee link a slightly different color or weight so it stands out
.special-nav-link {
    font-weight: 600;
    color: #d35400; /* A nice subtle orange/brand color */
}
</style>
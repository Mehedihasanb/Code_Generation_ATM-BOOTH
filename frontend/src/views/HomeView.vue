<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();

const navigateTo = (route: string) => {
    router.push(route);
};
const auth = useAuthStore();
</script>

<template>
    <main class="home-wrapper">
        <section class="panel hero-section">
            <h1 class="headline">Banking, elevated.</h1>
            <p class="muted subtitle">
                Manage your wealth with clarity and confidence. RhinoBank provides seamless, secure, and modern digital banking designed for your lifestyle.
            </p>
            
            <div class="button-group">
                <button type="button" class="btn primary-btn" @click="navigateTo('/register')">Open an Account</button>
                <button type="button" class="btn secondary-btn" @click="navigateTo('/login')">Sign In</button>
                <button type="button" class="btn secondary-btn" @click="navigateTo('/atm')">Use ATM</button>
            </div>
        </section>

        <!-- Features -->
        <section class="features-grid">
            <router-link
                v-if="auth.isApprovedCustomer"
                to="/accounts"
                class="panel feature-card feature-card-link">
                <div class="icon">🛡️</div>
                <h3>My Accounts</h3>
                <p class="muted">Review your balances, manage linked accounts, and keep track of your banking details in one place.</p>
            </router-link>
            
            <router-link
                v-if="auth.isApprovedCustomer"
                to="/transfer"
                class="panel feature-card feature-card-link">
                <div class="icon">⚡</div>
                <h3>Instant Transfers</h3>
                <p class="muted">Send and receive money globally in seconds. Enjoy borderless finance with zero hidden transaction fees.</p>
                
            </router-link>
            
            <router-link
                v-if="auth.isApprovedCustomer"
                to="/transactions"
                class="panel feature-card feature-card-link">
                <div class="icon">📱</div>
                <h3>History</h3>
                <p class="muted">Track your spending habits, filter by date, amount or receiver.</p>
            </router-link>

            <article class="panel feature-card">
                <div class="icon"> 👤</div>
                <h3>Manage your profile</h3>
                <p class="muted">Track your information and set your settings at any time.</p>
            </article>
        </section>
    </main>
</template>

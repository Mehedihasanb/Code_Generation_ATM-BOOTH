<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const navigateTo = (route: string) => {
    router.push(route);
};
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
            </div>
        </section>

        <!-- Features -->
        <section class="features-grid">
            <article class="panel feature-card">
                <div class="icon">🛡️</div>
                <h3>Secure Vault</h3>
                <p class="muted">Industry-leading encryption ensures your assets and personal data remain strictly confidential and protected 24/7.</p>
            </article>
            
            <article class="panel feature-card">
                <div class="icon">⚡</div>
                <h3>Instant Transfers</h3>
                <p class="muted">Send and receive money globally in seconds. Enjoy borderless finance with zero hidden transaction fees.</p>
            </article>
            
            <article class="panel feature-card">
                <div class="icon">📱</div>
                <h3>Smart Insights</h3>
                <p class="muted">Track your spending habits, set budgets, and grow your savings automatically with our AI-driven financial tools.</p>
            </article>

            <article class="panel feature-card">
                <div class="icon"> 👤</div>
                <h3>Manage your profile</h3>
                <p class="muted">Track your information and set your settings at any time.</p>
            </article>
        </section>
    </main>
</template>

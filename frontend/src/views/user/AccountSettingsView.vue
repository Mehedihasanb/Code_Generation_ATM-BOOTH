<script setup lang="ts">
import AccountDeletionPanel from '@/components/organisms/AccountDeletionPanel.vue';
</script>

<template>
    <main class="home-wrapper">
        <section class="panel hero-section">
            <h1 class="headline">Account settings</h1>
            <p class="muted subtitle">Deactivate or permanently delete your RhinoBank login.</p>
        </section>

        <section class="panel page-panel-wide">
            <AccountDeletionPanel mode="self" />
        </section>
    </main>
</template>

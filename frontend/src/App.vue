<script setup lang="ts">
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import Header from './components/organisms/Header.vue';
import Footer from './components/organisms/Footer.vue';

const route = useRoute();
// ATM pages use full-screen dark layout instead of normal bank header/footer
const isAtmLayout = computed(() => Boolean(route.meta.atmLayout));
</script>

<template>
	<div v-if="isAtmLayout" class="atm-layout">
		<RouterView />
	</div>
	<div v-else class="layout">
		<Header />
		<main class="main">
			<RouterView />
		</main>
		<Footer />
	</div>
</template>

<style scoped>
.atm-layout {
	min-height: 100vh;
}
</style>

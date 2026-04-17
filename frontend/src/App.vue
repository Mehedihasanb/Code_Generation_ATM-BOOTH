<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';
</script>

<template>
	<div class="layout">
		<header class="header">
			<strong class="brand">Code Generation</strong>
			<nav class="nav">
				<RouterLink to="/">Home</RouterLink>
			</nav>
		</header>
		<main class="main">
			<RouterView />
		</main>
	</div>
</template>

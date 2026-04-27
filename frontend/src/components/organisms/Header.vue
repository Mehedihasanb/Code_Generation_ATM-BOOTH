<script setup lang="ts">
import { RouterLink } from 'vue-router';
</script>

<template>
	<header class="header">
		<strong class="brand">Code Generation</strong>
		<nav class="nav">
			<RouterLink to="/">Home</RouterLink>
			<RouterLink to="/register">Register</RouterLink>
		</nav>
	</header>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const menuOpen = ref(false);

const brandTarget = computed(() => (auth.isEmployee ? '/service-desk' : '/'));

function toggleMenu() {
    menuOpen.value = !menuOpen.value;
}

function closeMenu() {
    menuOpen.value = false;
}

function handleLogout() {
    auth.logout();
    closeMenu();
    router.push('/');
}

watch(() => route.path, () => {
    closeMenu();
});
</script>

<template>
    <header
        class="header"
        :class="{
            'header-employee': auth.isEmployee,
            'header-customer': auth.isApprovedCustomer,
        }">
        <RouterLink :to="brandTarget" class="brand" @click="closeMenu">
            RhinoBank
            <span v-if="auth.isEmployee" class="brand-badge brand-badge--staff">Staff</span>
            <span v-else-if="auth.isApprovedCustomer" class="brand-badge brand-badge--customer">Banking</span>
        </RouterLink>

        <button
            type="button"
            class="nav-toggle"
            :aria-expanded="menuOpen"
            aria-controls="main-nav"
            aria-label="Toggle navigation menu"
            @click="toggleMenu">
            <span />
            <span />
            <span />
        </button>

        <nav
            id="main-nav"
            class="nav"
            :class="{ 'nav-open': menuOpen, 'nav-employee': auth.isEmployee }">
            <div class="nav-links">
                <template v-if="auth.isEmployee">
                    <div class="portal-nav-rail portal-nav-rail--employee" role="list">
                        <RouterLink
                            to="/service-desk"
                            class="portal-nav-item"
                            role="listitem"
                            @click="closeMenu">
                            <span class="portal-nav-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M9 11l3 3L22 4" />
                                    <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
                                </svg>
                            </span>
                            <span class="portal-nav-label">Approvals</span>
                        </RouterLink>

                        <RouterLink
                            to="/employee/transactions"
                            class="portal-nav-item"
                            role="listitem"
                            @click="closeMenu">
                            <span class="portal-nav-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M3 3v18h18" />
                                    <path d="M7 16l4-4 4 4 5-6" />
                                </svg>
                            </span>
                            <span class="portal-nav-label">Transactions</span>
                        </RouterLink>

                        <RouterLink
                            to="/employee/directory"
                            class="portal-nav-item"
                            role="listitem"
                            @click="closeMenu">
                            <span class="portal-nav-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                                    <circle cx="9" cy="7" r="4" />
                                    <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
                                    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                                </svg>
                            </span>
                            <span class="portal-nav-label">Directory</span>
                        </RouterLink>

                        <RouterLink
                            to="/employee/transfer"
                            class="portal-nav-item"
                            role="listitem"
                            @click="closeMenu">
                            <span class="portal-nav-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M7 17L17 7" />
                                    <path d="M7 7h10v10" />
                                </svg>
                            </span>
                            <span class="portal-nav-label">Force Transfer</span>
                        </RouterLink>
                    </div>
                </template>

                <template v-else-if="auth.isApprovedCustomer">
                    <div class="portal-nav-rail portal-nav-rail--customer" role="list">
                        <RouterLink
                            to="/"
                            class="portal-nav-item"
                            role="listitem"
                            active-class=""
                            exact-active-class="router-link-active"
                            @click="closeMenu">
                            <span class="portal-nav-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1V9.5z" />
                                </svg>
                            </span>
                            <span class="portal-nav-label">Home</span>
                        </RouterLink>

                        <RouterLink
                            to="/accounts"
                            class="portal-nav-item"
                            role="listitem"
                            @click="closeMenu">
                            <span class="portal-nav-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <rect x="2" y="5" width="20" height="14" rx="2" />
                                    <path d="M2 10h20" />
                                </svg>
                            </span>
                            <span class="portal-nav-label">My Accounts</span>
                        </RouterLink>

                        <RouterLink
                            to="/transactions"
                            class="portal-nav-item"
                            role="listitem"
                            @click="closeMenu">
                            <span class="portal-nav-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <circle cx="12" cy="12" r="9" />
                                    <path d="M12 7v5l3 2" />
                                </svg>
                            </span>
                            <span class="portal-nav-label">History</span>
                        </RouterLink>

                        <RouterLink
                            to="/transfer"
                            class="portal-nav-item"
                            role="listitem"
                            @click="closeMenu">
                            <span class="portal-nav-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M7 17h10" />
                                    <path d="M7 7h10" />
                                    <path d="M17 7l3 3-3 3" />
                                    <path d="M7 17L4 14l3-3" />
                                </svg>
                            </span>
                            <span class="portal-nav-label">Transfer</span>
                        </RouterLink>
                    </div>
                </template>

                <template v-else>
                    <RouterLink to="/" @click="closeMenu">Home</RouterLink>
                </template>
            </div>

            <div class="nav-user">
                <template v-if="auth.isAuthenticated">
                    <span class="greeting">Hello, {{ auth.firstName || 'User' }}</span>
                    <RouterLink to="/account/settings" class="btn-link nav-logout" @click="closeMenu">
                        Account
                    </RouterLink>
                    <button type="button" @click="handleLogout" class="btn-link nav-logout">Logout</button>
                </template>

                <template v-else>
                    <RouterLink to="/login" @click="closeMenu">Login</RouterLink>
                    <RouterLink to="/register" @click="closeMenu">Register</RouterLink>
                </template>
            </div>
        </nav>
    </header>
</template>

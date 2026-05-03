import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import RegistrationView from '../views/RegistrationView.vue';
import LoginView from '../views/LoginView.vue';
import PendingApprovalView from '../views/PendingApprovalView.vue';

const roleStorageKey = 'code-generation-role';
const approvedStorageKey = 'code-generation-approved';

const router = createRouter({
	history: createWebHistory(import.meta.env.BASE_URL),
	routes: [
		{
			path: '/',
			name: 'home',
			component: HomeView,
				meta: { requiresApproved: true },
		},
		{
			path: '/login',
			name: 'login',
			component: LoginView,
		},
			{
				path: '/register',
				name: 'register',
				component: RegistrationView,
			},
			{
				path: '/pending-approval',
				name: 'pending-approval',
				component: PendingApprovalView,
			},
	],
});

router.beforeEach((to) => {
	const role = localStorage.getItem(roleStorageKey);
	const approved = localStorage.getItem(approvedStorageKey) === 'true';
	const isPendingCustomer = role === 'CUSTOMER' && !approved;

	if (isPendingCustomer && to.path !== '/pending-approval' && to.meta.requiresApproved) {
		return '/pending-approval';
	}

	if (!isPendingCustomer && to.path === '/pending-approval') {
		return '/';
	}

	return true;
});

export default router;

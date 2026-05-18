import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import RegistrationView from '../views/user/RegistrationView.vue';
import LoginView from '../views/user/LoginView.vue';
import PendingApprovalView from '../views/user/PendingApprovalView.vue';
import ServiceDeskApprovalsView from '../views/employee/ServiceDeskApprovalsView.vue';
import {
	customerApprovalStatusStorageKey,
	roleStorageKey,
	tokenStorageKey,
} from '../stores/auth';

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
		{
			path: '/service-desk',
			name: 'service-desk',
			component: ServiceDeskApprovalsView,
			meta: { requiresEmployee: true },
		},
		{
			path: '/service-desk/approvals',
			name: 'service-desk-approvals',
			component: ServiceDeskApprovalsView,
			meta: { requiresEmployee: true },
		},
	],
});

router.beforeEach((to) => {
	const role = localStorage.getItem(roleStorageKey);
	const token = localStorage.getItem(tokenStorageKey);
	const customerApprovalStatus = localStorage.getItem(customerApprovalStatusStorageKey);
	const isPendingCustomer = role === 'CUSTOMER' && customerApprovalStatus === 'PENDING';

	if (to.meta.requiresEmployee) {
		if (!token || role !== 'EMPLOYEE') {
			return { path: '/login', query: { redirect: to.fullPath } };
		}
	}

	if (isPendingCustomer && to.path !== '/pending-approval' && to.meta.requiresApproved) {
		return '/pending-approval';
	}

	if (!isPendingCustomer && to.path === '/pending-approval') {
		return '/';
	}

	return true;
});

export default router;

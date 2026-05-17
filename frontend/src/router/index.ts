import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import RegistrationView from '../views/user/RegistrationView.vue';
import LoginView from '../views/user/LoginView.vue';
import PendingApprovalView from '../views/user/PendingApprovalView.vue';
import ServiceDeskApprovalsView from '../views/employee/ServiceDeskApprovalsView.vue';

const roleStorageKey = 'code-generation-role';
const customerApprovalStatusStorageKey = 'code-generation-customer-approval-status';

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
		},
		{
			path: '/service-desk/approvals',
			name: 'service-desk-approvals',
			component: ServiceDeskApprovalsView,
		},
	],
});

router.beforeEach((to) => {
	// `auth` store uses `sessionStorage`; keep router consistent so pending
	// customers are detected after login without a full reload.
	const role = sessionStorage.getItem(roleStorageKey);
	const customerApprovalStatus = sessionStorage.getItem(customerApprovalStatusStorageKey);
	const isPendingCustomer = role === 'CUSTOMER' && customerApprovalStatus === 'PENDING';

	if (isPendingCustomer && to.path !== '/pending-approval' && to.meta.requiresApproved) {
		return '/pending-approval';
	}

	if (!isPendingCustomer && to.path === '/pending-approval') {
		return '/';
	}

	return true;
});

export default router;

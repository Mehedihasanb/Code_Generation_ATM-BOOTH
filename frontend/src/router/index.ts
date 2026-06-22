import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import RegistrationView from '../views/user/RegistrationView.vue';
import LoginView from '../views/user/LoginView.vue';
import PendingApprovalView from '../views/user/PendingApprovalView.vue';
import ServiceDeskApprovalsView from '../views/employee/ServiceDeskApprovalsView.vue';
import CustomerAccountsView from '@/views/user/CustomerAccountsView.vue';
import CustomerTransferView from '@/views/user/CustomerTransferView.vue';
import EmployeeTransactionsView from '../views/employee/EmployeeTransactionsView.vue';
import EmployeeCustomerDirectoryView from '../views/employee/EmployeeCustomerDirectoryView.vue';
import EmployeeTransferView from '../views/employee/EmployeeTransferView.vue';
import AtmLoginView from '../views/atm/AtmLoginView.vue';
import AtmHomeView from '../views/atm/AtmHomeView.vue';
import AtmWithdrawView from '../views/atm/AtmWithdrawView.vue';
import AtmDepositView from '../views/atm/AtmDepositView.vue';

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
		{
			path: '/accounts',
			name: 'accounts',
			component: CustomerAccountsView,
			meta: { requiresApprovedCustomer: true },
		},
		{
			path: '/account/settings',
			name: 'account-settings',
			component: () => import('../views/user/AccountSettingsView.vue'),
			meta: { requiresAuth: true },
		},
		{
			path: '/transfer',
			name: 'transfer',
			component: CustomerTransferView,
			meta: { requiresApprovedCustomer: true },
		},
		{
		path: '/transactions',
		name: 'transactions',
		component: () => import('../views/user/CustomerTransactionsView.vue')
		},
		{
			path: '/employee/transactions',
			name: 'system-transactions',
            component: EmployeeTransactionsView,
            meta: { requiresEmployee: true } 
        },
		{
            path: '/employee/directory',
            name: 'employee-directory',
            component: EmployeeCustomerDirectoryView,
            meta: { requiresEmployee: true }
        },
		{
            path: '/employee/transfer',
            name: 'employee-transfer',
            component: EmployeeTransferView,
            meta: { requiresEmployee: true }
        },
		// ATM routes (separate layout, approved customers only after login)
		{
			path: '/atm',
			name: 'atm',
			redirect: '/atm/login',
		},
		{
			path: '/atm/login',
			name: 'atm-login',
			component: AtmLoginView,
			meta: { atmLayout: true },
		},
		{
			path: '/atm/home',
			name: 'atm-home',
			component: AtmHomeView,
			meta: { atmLayout: true, requiresAtmApprovedCustomer: true },
		},
		{
			path: '/atm/withdraw',
			name: 'atm-withdraw',
			component: AtmWithdrawView,
			meta: { atmLayout: true, requiresAtmApprovedCustomer: true },
		},
		{
			path: '/atm/deposit',
			name: 'atm-deposit',
			component: AtmDepositView,
			meta: { atmLayout: true, requiresAtmApprovedCustomer: true },
		},
	],
});

router.beforeEach((to) => {
	const token = sessionStorage.getItem(tokenStorageKey);
	const role = sessionStorage.getItem(roleStorageKey);
	const customerApprovalStatus = sessionStorage.getItem(customerApprovalStatusStorageKey);
	const isPendingCustomer = role === 'CUSTOMER' && customerApprovalStatus === 'PENDING';
	const isApprovedCustomer = role === 'CUSTOMER' && customerApprovalStatus === 'APPROVED';

	if (to.meta.requiresEmployee) {
		if (!token || role !== 'EMPLOYEE') {
			return { path: '/login', query: { redirect: to.fullPath } };
		}
	}

	if (to.meta.requiresAuth) {
		if (!token) {
			return { path: '/login', query: { redirect: to.fullPath } };
		}
	}

	if (to.meta.requiresApprovedCustomer) {
		if (!token || !isApprovedCustomer) {
			return { path: '/login', query: { redirect: to.fullPath } };
		}
	}

	// Withdraw/deposit need an approved customer JWT and send others to ATM login
	if (to.meta.requiresAtmApprovedCustomer) {
		if (!token || !isApprovedCustomer) {
			return { path: '/atm/login', query: { redirect: to.fullPath } };
		}
	}

	// Already logged in? Skip ATM login and go straight to the menu
	if (to.path === '/atm/login' && token && isApprovedCustomer) {
		return '/atm/home';
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

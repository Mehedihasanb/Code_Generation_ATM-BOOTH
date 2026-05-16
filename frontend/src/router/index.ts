import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import RegistrationView from '../views/RegistrationView.vue';
import PendingApprovalView from '../views/PendingApprovalView.vue';
import EmployeeDashboardView from '../views/EmployeeDashboardView.vue';
import { useAuthStore } from '@/stores/auth';

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView,
            // Anyone can access home
        },
        {
            path: '/login',
            name: 'login',
            component: LoginView,
            // If already logged in, we shouldn't see the login page
            meta: { requiresGuest: true } 
        },
        {
            path: '/register',
            name: 'register',
            component: RegistrationView,
            meta: { requiresGuest: true }
        },
        {
            path: '/pending-approval',
            name: 'pendingApproval',
            component: PendingApprovalView,
            // Must be logged in, but specifically a pending customer
            meta: { requiresAuth: true } 
        },
        {
            path: '/employee',
            name: 'employeeDashboard',
            component: EmployeeDashboardView,
            // This is the lock on the employee door
            meta: { requiresAuth: true, requiresEmployee: true } 
        }
    ]
});

// Global Navigation Guard
router.beforeEach((to, from, next) => {
    // IMPORTANT: useAuthStore must be called INSIDE the guard, 
    // otherwise Pinia will crash because it loads after the router
    const auth = useAuthStore(); 

    // 1. If route requires a guest (like Login/Register) but user is logged in
    if (to.meta.requiresGuest && auth.isAuthenticated) {
        return next('/'); 
    }

    // 2. If route requires authentication but user is NOT logged in
    if (to.meta.requiresAuth && !auth.isAuthenticated) {
        return next('/login');
    }

    // 3. If user is a pending customer, lock them to the pending page
    // (Unless they are trying to log out, etc.)
    if (auth.isAuthenticated && auth.isPendingCustomer && to.path !== '/pending-approval') {
        return next('/pending-approval');
    }

    // 4. If route requires an employee, but user is just a customer
    if (to.meta.requiresEmployee && auth.role !== 'EMPLOYEE') {
        return next('/'); 
    }

    // 5. If all checks pass, allow the navigation
    next();
});

export default router;
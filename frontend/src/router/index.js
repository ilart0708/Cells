import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ROLE_HOME } from '../constants/roles'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { roles: ['ADMIN'] },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('../views/AdminDashboardView.vue'),
        },
      ],
    },
    {
      path: '/pastor',
      component: () => import('../layouts/PastorLayout.vue'),
      meta: { roles: ['PASTOR'] },
      children: [
        {
          path: '',
          name: 'pastor-dashboard',
          component: () => import('../views/PastorDashboardView.vue'),
        },
      ],
    },
    {
      path: '/leader',
      component: () => import('../layouts/LeaderLayout.vue'),
      meta: { roles: ['LEADER'] },
      children: [
        {
          path: '',
          name: 'leader-dashboard',
          component: () => import('../views/LeaderDashboardView.vue'),
        },
      ],
    },
    {
      path: '/member',
      component: () => import('../layouts/MemberLayout.vue'),
      meta: { roles: ['MEMBER'] },
      children: [
        {
          path: '',
          name: 'member-dashboard',
          component: () => import('../views/MemberDashboardView.vue'),
        },
      ],
    },
    // "/" and unknown paths go to login. The guard below sends a logged-in user
    // from login to their own area, so no extra "home" route is needed.
    { path: '/', redirect: { name: 'login' } },
    { path: '/:pathMatch(.*)*', redirect: () => ({ name: 'login' }) },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    return to.name === 'login' && auth.isAuthenticated ? { name: ROLE_HOME[auth.role] } : true
  }

  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // A child route inherits the "roles" meta of its layout.
  const allowed = to.meta.roles
  if (allowed && !allowed.includes(auth.role)) {
    return { name: ROLE_HOME[auth.role] }
  }

  return true
})

export default router

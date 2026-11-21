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
        {
          path: 'churches',
          name: 'admin-churches',
          component: () => import('../views/admin/ChurchManagementView.vue'),
        },
        {
          path: 'pastors',
          name: 'admin-pastors',
          component: () => import('../views/admin/PastorManagementView.vue'),
        },
        {
          path: 'leaders',
          name: 'admin-leaders',
          component: () => import('../views/admin/LeaderManagementView.vue'),
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
        {
          path: 'leaders',
          name: 'pastor-leaders',
          component: () => import('../views/pastor/MyLeadersView.vue'),
        },
        {
          path: 'leaders/:leaderId(\\d+)/stats',
          name: 'pastor-leader-stats',
          component: () => import('../views/pastor/PastorLeaderStatsView.vue'),
          props: (route) => ({ leaderId: Number(route.params.leaderId) }),
        },
        {
          path: 'rooms/:roomId(\\d+)/stats',
          name: 'pastor-room-stats',
          component: () => import('../views/pastor/PastorRoomStatsView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
        },
        {
          path: 'reports',
          name: 'pastor-reports',
          component: () => import('../views/pastor/PastorReportsView.vue'),
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
        {
          path: 'rooms',
          name: 'leader-rooms',
          component: () => import('../views/leader/MyRoomsView.vue'),
        },
        {
          path: 'rooms/:roomId(\\d+)',
          name: 'leader-room',
          component: () => import('../views/leader/RoomDetailView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
        },
        {
          path: 'rooms/:roomId(\\d+)/devotions',
          name: 'leader-devotions',
          component: () => import('../views/leader/DevotionHistoryView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
        },
        {
          path: 'rooms/:roomId(\\d+)/devotions/new',
          name: 'leader-devotion-new',
          component: () => import('../views/leader/DevotionFormView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId), devotionId: null }),
        },
        {
          path: 'rooms/:roomId(\\d+)/devotions/:devotionId(\\d+)/edit',
          name: 'leader-devotion-edit',
          component: () => import('../views/leader/DevotionFormView.vue'),
          props: (route) => ({
            roomId: Number(route.params.roomId),
            devotionId: Number(route.params.devotionId),
          }),
        },
        {
          path: 'rooms/:roomId(\\d+)/today',
          name: 'leader-today',
          component: () => import('../views/leader/LeaderTodayView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
        },
        {
          path: 'rooms/:roomId(\\d+)/devotions/:devotionId(\\d+)/answers',
          name: 'leader-answers',
          component: () => import('../views/leader/LeaderAnswersView.vue'),
          props: (route) => ({
            roomId: Number(route.params.roomId),
            devotionId: Number(route.params.devotionId),
          }),
        },
        {
          path: 'rooms/:roomId(\\d+)/board',
          name: 'leader-board',
          component: () => import('../views/leader/LeaderBoardView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
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
          component: () => import('../views/member/MemberDashboardView.vue'),
        },
        {
          path: 'rooms/:roomId(\\d+)',
          name: 'member-room',
          component: () => import('../views/member/MemberRoomView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
        },
        {
          path: 'rooms/:roomId(\\d+)/devotion',
          name: 'member-devotion-current',
          component: () => import('../views/member/MemberCurrentDevotionView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
        },
        {
          path: 'rooms/:roomId(\\d+)/devotions',
          name: 'member-devotions',
          component: () => import('../views/member/MemberDevotionHistoryView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
        },
        {
          path: 'rooms/:roomId(\\d+)/devotions/:devotionId(\\d+)',
          name: 'member-devotion',
          component: () => import('../views/member/MemberDevotionView.vue'),
          props: (route) => ({
            roomId: Number(route.params.roomId),
            devotionId: Number(route.params.devotionId),
          }),
        },
        {
          path: 'experience',
          name: 'member-experience',
          component: () => import('../views/member/ExperienceView.vue'),
        },
        {
          path: 'history',
          name: 'member-history',
          component: () => import('../views/member/HistoryView.vue'),
        },
        {
          path: 'rooms/:roomId(\\d+)/history',
          name: 'member-history-room',
          component: () => import('../views/member/RoomHistoryView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
        },
        {
          path: 'rooms/:roomId(\\d+)/board',
          name: 'member-board',
          component: () => import('../views/member/BoardView.vue'),
          props: (route) => ({ roomId: Number(route.params.roomId) }),
        },
      ],
    },
    {
      path: '/accept-invitation',
      name: 'accept-invitation',
      component: () => import('../views/AcceptInvitationView.vue'),
      meta: { public: true },
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

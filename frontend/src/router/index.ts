import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import LoginView from '@/views/auth/LoginView.vue'
import DashboardView from '@/views/dashboard/DashboardView.vue'
import RequestListView from '@/views/requests/RequestListView.vue'
import RequestCreateView from '@/views/requests/RequestCreateView.vue'
import RequestDetailView from '@/views/requests/RequestDetailView.vue'
import WorkspaceView from '@/views/workspace/WorkspaceView.vue'
import MemberManagementView from '@/views/admin/MemberManagementView.vue'
import CategoryManagementView from '@/views/admin/CategoryManagementView.vue'
import StatisticsView from '@/views/admin/StatisticsView.vue'
import SettingsView from '@/views/settings/SettingsView.vue'
import NotificationCenterView from '@/views/notifications/NotificationCenterView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: DashboardView },
        { path: 'requests', name: 'request-list', component: RequestListView },
        {
          path: 'requests/new',
          name: 'request-create',
          component: RequestCreateView,
          meta: { roles: ['REQUESTER', 'ADMIN'] },
        },
        { path: 'requests/:id', name: 'request-detail', component: RequestDetailView },
        {
          path: 'workspace',
          name: 'workspace',
          component: WorkspaceView,
          meta: { roles: ['MEMBER', 'ADMIN'] },
        },
        {
          path: 'admin/members',
          component: MemberManagementView,
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/categories',
          component: CategoryManagementView,
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/statistics',
          component: StatisticsView,
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: NotificationCenterView,
        },
        { path: 'settings', component: SettingsView },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!authStore.initialized) {
    await authStore.loadCurrentUser()
  }
  if (!to.meta.public && !authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && authStore.isAuthenticated) {
    return '/dashboard'
  }

  const roles = to.meta.roles
  if (roles && authStore.user && !roles.includes(authStore.user.role)) {
    return '/dashboard'
  }
})

export default router

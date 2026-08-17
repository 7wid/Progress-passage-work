import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import { useAuthStore } from '@/stores/auth'

const LoginView = () => import('@/views/auth/LoginView.vue')
const RegisterView = () => import('@/views/auth/RegisterView.vue')
const DashboardView = () => import('@/views/dashboard/DashboardView.vue')
const RequestListView = () => import('@/views/requests/RequestListView.vue')
const RequestCreateView = () => import('@/views/requests/RequestCreateView.vue')
const RequestDetailView = () => import('@/views/requests/RequestDetailView.vue')
const WorkspaceView = () => import('@/views/workspace/WorkspaceView.vue')
const MemberManagementView = () => import('@/views/admin/MemberManagementView.vue')
const CategoryManagementView = () => import('@/views/admin/CategoryManagementView.vue')
const StatisticsView = () => import('@/views/admin/StatisticsView.vue')
const AuditLogView = () => import('@/views/admin/AuditLogView.vue')
const SettingsView = () => import('@/views/settings/SettingsView.vue')
const NotificationCenterView = () => import('@/views/notifications/NotificationCenterView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/register', name: 'register', component: RegisterView, meta: { public: true } },
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
        {
          path: 'requests/:id/edit',
          name: 'request-edit',
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
          name: 'admin-members',
          component: MemberManagementView,
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/categories',
          name: 'admin-categories',
          component: CategoryManagementView,
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/statistics',
          name: 'admin-statistics',
          component: StatisticsView,
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'admin/audit-logs',
          name: 'admin-audit-logs',
          component: AuditLogView,
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
  if ((to.path === '/login' || to.path === '/register') && authStore.isAuthenticated) {
    return '/dashboard'
  }

  const roles = to.meta.roles
  if (roles && authStore.user && !roles.includes(authStore.user.role)) {
    return '/dashboard'
  }
})

export default router

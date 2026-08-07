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
        { path: 'requests', component: RequestListView },
        { path: 'requests/new', component: RequestCreateView },
        { path: 'requests/:id', component: RequestDetailView },
        { path: 'workspace', component: WorkspaceView },
        { path: 'admin/members', component: MemberManagementView },
        { path: 'admin/categories', component: CategoryManagementView },
        { path: 'admin/statistics', component: StatisticsView },
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
})

export default router

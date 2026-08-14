<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'

const router = useRouter()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const isRequester = computed(() => authStore.user?.role === 'REQUESTER')

const isMember = computed(() => authStore.user?.role === 'MEMBER')

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

const isTeam = computed(() => isMember.value || isAdmin.value)

const canCreateRequest = computed(() => isRequester.value || isAdmin.value)

async function handleLogout() {
  try {
    await authStore.signOut()
    ElMessage.success('已退出登录')
  } catch {
    ElMessage.warning('退出请求失败，本地登录状态已清除')
  } finally {
    notificationStore.reset()
    await router.replace('/login')
  }
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="220px" class="app-shell__aside">
      <h1>技术需求管理</h1>
      <nav>
        <RouterLink to="/dashboard"> 首页 </RouterLink>

        <RouterLink to="/requests">
          {{ isRequester ? '我的需求' : '需求池' }}
        </RouterLink>

        <RouterLink v-if="canCreateRequest" to="/requests/new"> 提交需求 </RouterLink>

        <RouterLink v-if="isTeam" to="/workspace"> 技术组工作台 </RouterLink>

        <RouterLink v-if="isAdmin" to="/admin/members"> 成员管理 </RouterLink>

        <RouterLink v-if="isAdmin" to="/admin/categories"> 分类管理 </RouterLink>

        <RouterLink v-if="isAdmin" to="/admin/statistics"> 数据概览 </RouterLink>

        <RouterLink to="/settings"> 个人设置 </RouterLink>
      </nav>
    </el-aside>
    <el-container>
      <el-header class="app-shell__header">
        <span>计算机技术组外包需求管理系统</span>

        <div class="app-shell__user">
          <NotificationBell />
          <span>{{ authStore.user?.displayName }}</span>
          <el-button link type="primary" @click="handleLogout"> 退出登录 </el-button>
        </div>
      </el-header>
      <el-main>
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-shell__aside {
  padding: 24px 16px;
  color: #fff;
  background: #1f2d3d;
}

.app-shell__aside h1 {
  margin: 0 0 24px;
  font-size: 18px;
}

.app-shell__aside nav {
  display: grid;
  gap: 8px;
}

.app-shell__aside a {
  padding: 10px 12px;
  border-radius: 6px;
}

.app-shell__aside a.router-link-active {
  background: #409eff;
}

.app-shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.app-shell__user {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const mobileNavOpen = ref(false)

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

watch(
  () => route.fullPath,
  () => {
    mobileNavOpen.value = false
  },
)
</script>

<template>
  <el-container class="app-shell">
    <el-aside
      width="220px"
      class="app-shell__aside"
      :class="{ 'app-shell__aside--open': mobileNavOpen }"
    >
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

        <RouterLink v-if="isAdmin" to="/admin/audit-logs"> 审计记录 </RouterLink>

        <RouterLink to="/settings"> 个人设置 </RouterLink>
      </nav>
    </el-aside>
    <button
      v-if="mobileNavOpen"
      type="button"
      class="app-shell__backdrop"
      aria-label="关闭导航"
      @click="mobileNavOpen = false"
    />
    <el-container>
      <el-header class="app-shell__header">
        <div class="app-shell__brand">
          <button
            type="button"
            class="app-shell__menu-button"
            aria-label="打开导航"
            title="打开导航"
            @click="mobileNavOpen = true"
          >
            ☰
          </button>
          <span>计算机技术组外包需求管理系统</span>
        </div>

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

.app-shell__brand {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.app-shell__menu-button {
  display: none;
  width: 36px;
  height: 36px;
  padding: 0;
  font-size: 22px;
  color: #374151;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.app-shell__backdrop {
  display: none;
}

.app-shell__user {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 760px) {
  .app-shell__aside {
    position: fixed;
    z-index: 30;
    inset: 0 auto 0 0;
    transform: translateX(-100%);
    transition: transform 160ms ease;
  }

  .app-shell__aside--open {
    transform: translateX(0);
  }

  .app-shell__backdrop {
    position: fixed;
    z-index: 20;
    inset: 0;
    display: block;
    padding: 0;
    background: rgb(0 0 0 / 45%);
    border: 0;
  }

  .app-shell__menu-button {
    display: inline-grid;
    place-items: center;
  }

  .app-shell__header {
    height: auto;
    min-height: 60px;
    padding: 10px 12px;
    gap: 8px;
  }

  .app-shell__brand > span {
    display: none;
  }

  .app-shell__user {
    min-width: 0;
    gap: 8px;
  }

  .app-shell__user > span {
    max-width: 110px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.el-main) {
    padding: 14px;
  }
}
</style>

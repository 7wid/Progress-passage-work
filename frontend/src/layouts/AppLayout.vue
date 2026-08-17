<script setup lang="ts">
import { computed, markRaw, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { Component } from 'vue'
import {
  BarChart3,
  Bell,
  Braces,
  ChevronDown,
  ClipboardList,
  FolderKanban,
  LayoutDashboard,
  LogOut,
  Menu,
  Plus,
  ScrollText,
  Search,
  Settings,
  Tags,
  UsersRound,
  X,
} from '@lucide/vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'

interface NavigationItem {
  label: string
  to: string
  icon: Component
  visible?: boolean
}

interface NavigationGroup {
  label: string
  items: NavigationItem[]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const mobileNavOpen = ref(false)
const globalSearch = ref('')

const isRequester = computed(() => authStore.user?.role === 'REQUESTER')
const isMember = computed(() => authStore.user?.role === 'MEMBER')
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const isTeam = computed(() => isMember.value || isAdmin.value)
const canCreateRequest = computed(() => isRequester.value || isAdmin.value)

const roleLabel = computed(() => {
  switch (authStore.user?.role) {
    case 'ADMIN':
      return '系统管理员'
    case 'MEMBER':
      return '技术组成员'
    default:
      return '需求方'
  }
})

const userInitial = computed(
  () => authStore.user?.displayName?.trim().slice(0, 1).toUpperCase() || 'U',
)

const navigationGroups = computed<NavigationGroup[]>(() => [
  {
    label: '工作区',
    items: [
      { label: '概览', to: '/dashboard', icon: markRaw(LayoutDashboard) },
      {
        label: isRequester.value ? '我的需求' : '需求池',
        to: '/requests',
        icon: markRaw(ClipboardList),
      },
      {
        label: '技术组工作台',
        to: '/workspace',
        icon: markRaw(FolderKanban),
        visible: isTeam.value,
      },
    ],
  },
  {
    label: '组织管理',
    items: [
      {
        label: '成员管理',
        to: '/admin/members',
        icon: markRaw(UsersRound),
        visible: isAdmin.value,
      },
      { label: '分类管理', to: '/admin/categories', icon: markRaw(Tags), visible: isAdmin.value },
      {
        label: '数据概览',
        to: '/admin/statistics',
        icon: markRaw(BarChart3),
        visible: isAdmin.value,
      },
      {
        label: '审计记录',
        to: '/admin/audit-logs',
        icon: markRaw(ScrollText),
        visible: isAdmin.value,
      },
    ],
  },
  {
    label: '个人',
    items: [{ label: '个人设置', to: '/settings', icon: markRaw(Settings) }],
  },
])

const visibleNavigationGroups = computed(() =>
  navigationGroups.value
    .map((group) => ({ ...group, items: group.items.filter((item) => item.visible !== false) }))
    .filter((group) => group.items.length > 0),
)

const currentSection = computed(() => {
  const item = visibleNavigationGroups.value
    .flatMap((group) => group.items)
    .filter((entry) => route.path === entry.to || route.path.startsWith(`${entry.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0]
  if (route.path === '/requests/new') return '提交需求'
  return item?.label ?? '需求协作'
})

function submitGlobalSearch() {
  const keyword = globalSearch.value.trim()
  void router.push({ name: 'request-list', query: keyword ? { keyword } : {} })
}

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
  <a class="skip-link" href="#main-content">跳到主要内容</a>
  <div class="app-shell" :class="{ 'app-shell--nav-open': mobileNavOpen }">
    <aside id="primary-navigation" class="app-sidebar" aria-label="主导航">
      <div class="app-sidebar__brand">
        <RouterLink to="/dashboard" class="brand-link" aria-label="技术需求管理首页">
          <span class="brand-mark" aria-hidden="true"><Braces :size="20" :stroke-width="2" /></span>
          <span class="brand-copy">
            <strong>技术需求管理</strong>
            <small>TECH REQUESTS</small>
          </span>
        </RouterLink>
        <button
          type="button"
          class="sidebar-close"
          aria-label="关闭导航"
          title="关闭导航"
          @click="mobileNavOpen = false"
        >
          <X :size="20" aria-hidden="true" />
        </button>
      </div>

      <nav class="sidebar-nav">
        <section v-for="group in visibleNavigationGroups" :key="group.label" class="nav-group">
          <h2>{{ group.label }}</h2>
          <RouterLink v-for="item in group.items" :key="item.to" :to="item.to" class="nav-item">
            <component :is="item.icon" :size="19" :stroke-width="1.8" aria-hidden="true" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </section>
      </nav>
    </aside>

    <button
      v-if="mobileNavOpen"
      type="button"
      class="app-shell__backdrop"
      aria-label="关闭导航"
      @click="mobileNavOpen = false"
    />

    <div class="app-workspace">
      <header class="app-header">
        <div class="app-header__context">
          <button
            type="button"
            class="icon-button app-header__menu"
            aria-label="打开导航"
            aria-controls="primary-navigation"
            :aria-expanded="mobileNavOpen"
            title="打开导航"
            @click="mobileNavOpen = true"
          >
            <Menu :size="21" aria-hidden="true" />
          </button>
          <span>{{ currentSection }}</span>
        </div>

        <form class="global-search" role="search" @submit.prevent="submitGlobalSearch">
          <Search :size="17" aria-hidden="true" />
          <label class="sr-only" for="global-search-input">全局搜索需求</label>
          <input
            id="global-search-input"
            v-model="globalSearch"
            type="search"
            autocomplete="off"
            placeholder="搜索需求编号或标题"
          />
          <kbd>Enter</kbd>
        </form>

        <div class="app-header__actions">
          <el-button
            v-if="canCreateRequest"
            class="header-create"
            type="primary"
            @click="router.push('/requests/new')"
          >
            <Plus :size="17" aria-hidden="true" />
            <span>提交需求</span>
          </el-button>
          <NotificationBell />

          <el-dropdown trigger="click" placement="bottom-end">
            <button type="button" class="user-menu" aria-label="打开用户菜单">
              <span class="user-avatar" aria-hidden="true">{{ userInitial }}</span>
              <span class="user-copy">
                <strong>{{ authStore.user?.displayName }}</strong>
                <small>{{ roleLabel }}</small>
              </span>
              <ChevronDown :size="15" aria-hidden="true" />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/notifications')">
                  <Bell :size="16" aria-hidden="true" />
                  通知中心
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/settings')">
                  <Settings :size="16" aria-hidden="true" />
                  个人设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <LogOut :size="16" aria-hidden="true" />
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main id="main-content" class="app-main" tabindex="-1">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  min-height: 100dvh;
  background: var(--color-background);
}

.app-sidebar {
  position: sticky;
  z-index: var(--z-sticky);
  top: 0;
  display: flex;
  height: 100dvh;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border-subtle);
}

.app-sidebar__brand {
  display: flex;
  height: 68px;
  flex: 0 0 68px;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.brand-link {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 11px;
}

.brand-mark {
  display: inline-grid;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  place-items: center;
  color: var(--color-on-primary);
  background: var(--color-primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-brand);
}

.brand-copy {
  display: grid;
  min-width: 0;
  gap: 2px;
  white-space: nowrap;
}

.brand-copy strong {
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 650;
}

.brand-copy small {
  color: var(--color-text-tertiary);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0;
}

.sidebar-close {
  display: none;
}

.sidebar-nav {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 24px;
  overflow-y: auto;
  padding: 22px 12px;
}

.nav-group {
  display: grid;
  gap: 4px;
}

.nav-group h2 {
  margin: 0 10px 6px;
  color: var(--color-text-tertiary);
  font-size: 11px;
  font-weight: 650;
  line-height: 20px;
  white-space: nowrap;
}

.nav-item {
  position: relative;
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  transition:
    color var(--motion-fast) ease,
    background-color var(--motion-fast) ease;
}

.nav-item:hover {
  color: var(--color-text-primary);
  background: var(--color-surface-hover);
}

.nav-item.router-link-active {
  color: var(--color-primary-strong);
  background: var(--color-primary-soft);
  font-weight: 600;
}

.nav-item.router-link-active::before {
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 0;
  width: 3px;
  content: '';
  background: var(--color-primary);
  border-radius: 0 3px 3px 0;
}

.nav-item svg {
  flex: 0 0 auto;
}

.app-workspace {
  min-width: 0;
}

.app-header {
  position: sticky;
  z-index: var(--z-header);
  top: 0;
  display: grid;
  height: 68px;
  grid-template-columns: minmax(120px, 1fr) minmax(260px, 440px) minmax(260px, 1fr);
  align-items: center;
  gap: 20px;
  padding: 0 28px;
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  border-bottom: 1px solid var(--color-border-subtle);
  backdrop-filter: blur(12px);
}

.app-header__context {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 600;
}

.app-header__context::before {
  width: 4px;
  height: 17px;
  flex: 0 0 4px;
  content: '';
  background: var(--color-primary);
  border-radius: 999px;
}

.app-header__menu {
  display: none;
}

.global-search {
  display: flex;
  height: 40px;
  align-items: center;
  gap: 9px;
  padding: 0 12px;
  color: var(--color-text-tertiary);
  background: var(--color-surface-secondary);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  transition:
    background-color var(--motion-fast) ease,
    border-color var(--motion-fast) ease,
    box-shadow var(--motion-fast) ease;
}

.global-search:focus-within {
  background: var(--color-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-focus-ring);
}

.global-search input {
  min-width: 0;
  flex: 1;
  color: var(--color-text-primary);
  background: transparent;
  border: 0;
  outline: 0;
  font: inherit;
  font-size: 14px;
}

.global-search input::placeholder {
  color: var(--color-text-tertiary);
}

.global-search input::-webkit-search-cancel-button {
  cursor: pointer;
}

.global-search kbd {
  padding: 2px 6px;
  color: var(--color-text-tertiary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 5px;
  font-family: inherit;
  font-size: 10px;
  line-height: 16px;
}

.app-header__actions {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.header-create :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.user-menu {
  display: flex;
  min-width: 0;
  min-height: 44px;
  align-items: center;
  gap: 9px;
  padding: 4px 7px;
  color: var(--color-text-primary);
  background: transparent;
  border: 0;
  border-radius: var(--radius-md);
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.user-menu:hover {
  background: var(--color-surface-hover);
}

.user-avatar {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  color: var(--color-primary-strong);
  background: var(--color-primary-soft);
  border: 1px solid var(--color-primary-border);
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
}

.user-copy {
  display: grid;
  max-width: 120px;
  min-width: 0;
  gap: 1px;
}

.user-copy strong,
.user-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-copy strong {
  font-size: 13px;
  font-weight: 600;
}

.user-copy small {
  color: var(--color-text-tertiary);
  font-size: 11px;
}

.app-main {
  width: 100%;
  max-width: 1500px;
  min-width: 0;
  min-height: calc(100dvh - 68px);
  margin: 0 auto;
  padding: 30px 32px 48px;
  outline: none;
}

.app-shell__backdrop {
  display: none;
}

@media (max-width: 1180px) {
  .app-header {
    grid-template-columns: minmax(120px, 1fr) minmax(240px, 360px) auto;
    padding-inline: 20px;
  }

  .header-create span,
  .user-copy {
    display: none;
  }
}

@media (max-width: 900px) {
  .app-shell {
    display: block;
  }

  .app-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: min(288px, calc(100vw - 56px));
    transform: translateX(-100%);
    box-shadow: var(--shadow-drawer);
    transition: transform var(--motion-base) var(--ease-standard);
  }

  .app-shell--nav-open .app-sidebar {
    transform: translateX(0);
  }

  .sidebar-close {
    display: inline-grid;
    width: 40px;
    height: 40px;
    flex: 0 0 40px;
    place-items: center;
    color: var(--color-text-secondary);
    background: transparent;
    border: 0;
    border-radius: var(--radius-md);
    cursor: pointer;
  }

  .app-shell__backdrop {
    position: fixed;
    z-index: calc(var(--z-sticky) - 1);
    inset: 0;
    display: block;
    padding: 0;
    background: rgb(15 23 42 / 48%);
    border: 0;
  }

  .app-header {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .app-header__menu {
    display: inline-grid;
  }

  .global-search {
    display: none;
  }

  .app-main {
    padding: 24px 20px 40px;
  }
}

@media (max-width: 600px) {
  .app-header {
    height: 60px;
    gap: 8px;
    padding-inline: 14px;
  }

  .app-header__context > span {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .app-header__context::before {
    display: none;
  }

  .header-create {
    display: none;
  }

  .user-menu > svg {
    display: none;
  }

  .app-main {
    min-height: calc(100dvh - 60px);
    padding: 20px 16px 36px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-sidebar {
    transition: none;
  }
}
</style>

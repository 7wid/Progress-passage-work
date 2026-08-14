<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useNotificationStore } from '@/stores/notifications'

const notificationStore = useNotificationStore()
const POLL_INTERVAL_MS = 60_000
let pollingTimer: number | null = null

const accessibleLabel = computed(() =>
  notificationStore.hasUnread
    ? `通知，${notificationStore.unreadCount} 条未读`
    : '通知，无未读消息',
)

async function refreshUnreadCount(): Promise<void> {
  try {
    await notificationStore.refreshUnreadCount()
  } catch {
    // 轮询失败不打断当前操作，进入通知中心后仍会显示可重试的错误状态。
  }
}

function pollWhenVisible(): void {
  if (document.visibilityState === 'visible') void refreshUnreadCount()
}

function startPolling(): void {
  if (pollingTimer !== null) return
  pollingTimer = window.setInterval(pollWhenVisible, POLL_INTERVAL_MS)
}

function stopPolling(): void {
  if (pollingTimer === null) return
  window.clearInterval(pollingTimer)
  pollingTimer = null
}

function handleVisibilityChange(): void {
  if (document.visibilityState === 'visible') void refreshUnreadCount()
}

onMounted(() => {
  void refreshUnreadCount()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  startPolling()
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  stopPolling()
})
</script>

<template>
  <RouterLink
    :to="{ name: 'notifications' }"
    class="notification-link"
    :aria-label="accessibleLabel"
  >
    <el-badge
      :value="notificationStore.unreadCount"
      :max="99"
      :hidden="!notificationStore.hasUnread"
    >
      <span class="notification-trigger">通知</span>
    </el-badge>
  </RouterLink>
</template>

<style scoped>
.notification-link {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 4px 8px;
  border-radius: 6px;
  color: #409eff;
}

.notification-link:hover,
.notification-link:focus-visible {
  background: #ecf5ff;
  outline: none;
}

.notification-trigger {
  display: inline-block;
  line-height: 24px;
}
</style>

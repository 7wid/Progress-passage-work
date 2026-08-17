<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { Bell } from '@lucide/vue'
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
      <span class="notification-trigger" title="通知中心">
        <Bell :size="19" :stroke-width="1.8" aria-hidden="true" />
      </span>
    </el-badge>
  </RouterLink>
</template>

<style scoped>
.notification-link {
  display: inline-grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition:
    color var(--motion-fast) ease,
    background-color var(--motion-fast) ease;
}

.notification-link:hover,
.notification-link:focus-visible {
  color: var(--color-text-primary);
  background: var(--color-surface-hover);
}

.notification-trigger {
  display: inline-grid;
  width: 32px;
  height: 32px;
  place-items: center;
}

.notification-link :deep(.el-badge__content) {
  border: 2px solid var(--color-surface);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { Component } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Activity,
  ArrowRight,
  Bell,
  BellOff,
  BellRing,
  Check,
  CheckCheck,
  CircleAlert,
  ClipboardCheck,
  FileText,
  PackageCheck,
  RefreshCw,
} from '@lucide/vue'
import { useRouter } from 'vue-router'
import { getNotifications } from '@/api/notifications'
import { getApiErrorMessage } from '@/api/http'
import AppPageHeader from '@/components/common/AppPageHeader.vue'
import { useNotificationStore } from '@/stores/notifications'
import type { NotificationRecord } from '@/types/notification'

type NotificationFilter = 'ALL' | 'UNREAD'

const router = useRouter()
const notificationStore = useNotificationStore()
const activeFilter = ref<NotificationFilter>('ALL')
const loading = ref(false)
const errorMessage = ref('')
const items = ref<NotificationRecord[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)
const openingId = ref<string | null>(null)
let loadSequence = 0

const canMarkAll = computed(
  () =>
    !loading.value &&
    openingId.value === null &&
    !notificationStore.markAllLoading &&
    (notificationStore.hasUnread || items.value.some((item) => !item.read)),
)

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

type NotificationVisual = {
  icon: Component
  tone: 'blue' | 'green' | 'orange' | 'red' | 'purple'
}

function notificationVisual(type: string): NotificationVisual {
  if (type.includes('DELIVERY') || type.includes('ACCEPTANCE')) {
    return { icon: PackageCheck, tone: 'green' }
  }
  if (type.includes('PROGRESS') || type.includes('ASSIGNMENT')) {
    return { icon: Activity, tone: 'blue' }
  }
  if (type.includes('REJECT') || type.includes('CANCEL')) {
    return { icon: CircleAlert, tone: 'red' }
  }
  if (type.includes('EVALUATION')) {
    return { icon: ClipboardCheck, tone: 'purple' }
  }
  if (type.includes('REQUEST')) {
    return { icon: FileText, tone: 'orange' }
  }
  return { icon: Bell, tone: 'blue' }
}

function formatDateTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

async function loadNotifications(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await getNotifications({
      page: page.value,
      pageSize,
      unreadOnly: activeFilter.value === 'UNREAD',
    })
    if (sequence !== loadSequence) return
    items.value = result.items
    total.value = result.total
  } catch (error) {
    if (sequence !== loadSequence) return
    items.value = []
    total.value = 0
    errorMessage.value = getApiErrorMessage(error, '通知加载失败，请稍后重试')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function replaceNotification(updated: NotificationRecord): void {
  const index = items.value.findIndex((item) => item.id === updated.id)
  if (index >= 0) items.value.splice(index, 1, updated)
}

async function markOne(notification: NotificationRecord, navigate: boolean): Promise<void> {
  if (openingId.value !== null || notificationStore.isMarking(notification.id)) return
  openingId.value = notification.id

  try {
    const willNavigate = navigate && notification.requestId !== null
    if (!notification.read) {
      const updated = await notificationStore.markRead(notification)
      // 使已发出但尚未返回的旧列表请求失效，避免覆盖刚写入的已读状态。
      loadSequence += 1
      loading.value = false
      replaceNotification(updated)

      if (!willNavigate) {
        if (activeFilter.value === 'UNREAD' && items.value.length === 1 && page.value > 1) {
          page.value -= 1
        }
        await loadNotifications()
      }
    }

    if (willNavigate) {
      await router.push({
        name: 'request-detail',
        params: { id: notification.requestId! },
      })
    }
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '通知状态更新失败，请稍后重试'))
  } finally {
    openingId.value = null
  }
}

async function markAll(): Promise<void> {
  if (!canMarkAll.value) return

  try {
    const result = await notificationStore.markAllRead()
    page.value = 1
    await loadNotifications()
    ElMessage.success(
      result.updatedCount > 0 ? `已将 ${result.updatedCount} 条通知标为已读` : '没有新的未读通知',
    )
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '全部标为已读失败，请稍后重试'))
  }
}

function changePage(value: number): void {
  page.value = value
  void loadNotifications()
}

watch(activeFilter, () => {
  page.value = 1
  void loadNotifications()
})

onMounted(() => {
  void notificationStore.refreshUnreadCount().catch(() => undefined)
  void loadNotifications()
})
</script>

<template>
  <section class="page">
    <AppPageHeader
      title="站内通知"
      description="集中查看需求流转、协作与交付动态。"
      eyebrow="INBOX"
      :icon="BellRing"
      tone="orange"
    >
      <template #meta>
        <span class="unread-summary">
          <span aria-hidden="true" />{{ notificationStore.unreadCount }} 条未读
        </span>
      </template>
      <template #actions>
        <el-button
          data-test="refresh"
          :loading="loading"
          :disabled="openingId !== null || notificationStore.markAllLoading"
          @click="loadNotifications"
        >
          <RefreshCw :size="16" aria-hidden="true" />
          刷新
        </el-button>
        <el-button
          data-test="mark-all"
          type="primary"
          plain
          :loading="notificationStore.markAllLoading"
          :disabled="!canMarkAll"
          @click="markAll"
        >
          <CheckCheck :size="16" aria-hidden="true" />
          全部标为已读
        </el-button>
      </template>
    </AppPageHeader>

    <div class="notification-toolbar">
      <el-tabs v-model="activeFilter">
        <el-tab-pane label="全部通知" name="ALL" />
        <el-tab-pane label="仅看未读" name="UNREAD" />
      </el-tabs>
      <span>共 {{ total }} 条</span>
    </div>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadNotifications">重新加载</el-button>
      </template>
    </el-alert>

    <el-card v-loading="loading" class="notification-card">
      <el-empty v-if="!loading && items.length === 0" description="暂无通知">
        <template #image>
          <span class="empty-notification-icon" aria-hidden="true"><BellOff :size="28" /></span>
        </template>
      </el-empty>

      <ul v-else class="notification-list" aria-live="polite">
        <li
          v-for="notification in items"
          :key="notification.id"
          :data-notification-id="notification.id"
          class="notification-item"
          :class="{ 'notification-item--unread': !notification.read }"
        >
          <span
            class="notification-icon"
            :class="`notification-icon--${notificationVisual(notification.type).tone}`"
            aria-hidden="true"
          >
            <component :is="notificationVisual(notification.type).icon" :size="19" />
          </span>
          <button
            class="notification-main"
            type="button"
            :disabled="openingId !== null"
            @click="markOne(notification, true)"
          >
            <span class="notification-heading">
              <strong>{{ notification.title }}</strong>
              <span v-if="!notification.read" class="unread-dot">未读</span>
            </span>
            <span class="notification-content">{{ notification.content }}</span>
            <span class="notification-meta">
              <time :datetime="notification.createdAt">
                {{ formatDateTime(notification.createdAt) }}
              </time>
              <span v-if="notification.requestId">
                查看关联需求<ArrowRight :size="13" aria-hidden="true" />
              </span>
            </span>
          </button>

          <el-button
            v-if="!notification.read"
            link
            type="primary"
            :loading="notificationStore.isMarking(notification.id)"
            :disabled="openingId !== null || notificationStore.markAllLoading"
            @click="markOne(notification, false)"
          >
            <Check :size="15" aria-hidden="true" />
            标为已读
          </el-button>
        </li>
      </ul>

      <el-pagination
        v-if="total > pageSize"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        class="pagination"
        @current-change="changePage"
      />
    </el-card>
  </section>
</template>

<style scoped>
.unread-summary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--color-warning);
  font-size: 12px;
  font-weight: 650;
}

.unread-summary > span {
  width: 7px;
  height: 7px;
  background: var(--color-warning);
  border-radius: 50%;
  box-shadow: 0 0 0 3px #ffedd5;
}

.notification-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 0 4px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.notification-toolbar :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.notification-toolbar :deep(.el-tabs__header) {
  border-bottom: 0;
}

.notification-toolbar > span {
  padding-bottom: 12px;
  color: var(--color-text-tertiary);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.notification-card {
  overflow: hidden;
}

.notification-card :deep(.el-card__body) {
  padding: 0;
}

.notification-list {
  display: grid;
  margin: 0;
  padding: 0;
  list-style: none;
}

.notification-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-height: 108px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-surface);
  transition:
    background-color var(--motion-fast) ease,
    box-shadow var(--motion-fast) ease;
}

.notification-item:last-child {
  border-bottom: 0;
}

.notification-item:hover {
  z-index: 1;
  background: #fbfcff;
  box-shadow: inset 3px 0 0 var(--color-primary);
}

.notification-item--unread {
  background: #f6f9ff;
}

.notification-item--unread::before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 3px;
  background: var(--color-primary);
  content: '';
}

.notification-icon {
  display: inline-grid;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  place-items: center;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-radius: var(--radius-md);
}

.notification-icon--green {
  color: var(--color-success);
  background: #ecfdf5;
}

.notification-icon--orange {
  color: var(--color-warning);
  background: #fff7ed;
}

.notification-icon--red {
  color: var(--color-danger);
  background: #fef2f2;
}

.notification-icon--purple {
  color: var(--color-purple);
  background: #f5f3ff;
}

.notification-main {
  display: grid;
  flex: 1;
  min-width: 0;
  gap: 5px;
  padding: 0;
  border: 0;
  color: inherit;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.notification-main:disabled {
  cursor: wait;
}

.notification-heading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notification-heading strong {
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 650;
}

.unread-dot {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 650;
}

.unread-dot::before {
  width: 5px;
  height: 5px;
  background: currentcolor;
  border-radius: 50%;
  content: '';
}

.notification-content {
  overflow-wrap: anywhere;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.notification-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px 14px;
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.notification-meta > span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--color-primary-strong);
  font-weight: 550;
}

.notification-item > :deep(.el-button) {
  flex: 0 0 auto;
  margin-top: 3px;
}

.notification-item > :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.empty-notification-icon {
  display: inline-grid;
  width: 64px;
  height: 64px;
  place-items: center;
  color: var(--color-text-tertiary);
  background: var(--color-surface-secondary);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
}

.pagination {
  justify-content: flex-end;
  padding: 16px 18px;
  border-top: 1px solid var(--color-border-subtle);
}

@media (max-width: 640px) {
  .notification-item {
    align-items: stretch;
    flex-direction: column;
  }

  .notification-item {
    min-height: 0;
    padding: 16px;
  }

  .notification-icon {
    width: 36px;
    height: 36px;
    flex-basis: 36px;
  }

  .pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>

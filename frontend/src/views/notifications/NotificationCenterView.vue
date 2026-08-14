<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getNotifications } from '@/api/notifications'
import { getApiErrorMessage } from '@/api/http'
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
    <div class="page__header">
      <div>
        <h2>站内通知</h2>
        <span class="summary">当前有 {{ notificationStore.unreadCount }} 条未读通知</span>
      </div>

      <div class="header-actions">
        <el-button
          data-test="refresh"
          :loading="loading"
          :disabled="openingId !== null || notificationStore.markAllLoading"
          @click="loadNotifications"
        >
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
          全部标为已读
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeFilter">
      <el-tab-pane label="全部通知" name="ALL" />
      <el-tab-pane label="仅看未读" name="UNREAD" />
    </el-tabs>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadNotifications">重新加载</el-button>
      </template>
    </el-alert>

    <el-card v-loading="loading">
      <el-empty v-if="!loading && items.length === 0" description="暂无通知" />

      <ul v-else class="notification-list" aria-live="polite">
        <li
          v-for="notification in items"
          :key="notification.id"
          :data-notification-id="notification.id"
          class="notification-item"
          :class="{ 'notification-item--unread': !notification.read }"
        >
          <button
            class="notification-main"
            type="button"
            :disabled="openingId !== null"
            @click="markOne(notification, true)"
          >
            <span class="notification-heading">
              <strong>{{ notification.title }}</strong>
              <el-tag v-if="!notification.read" size="small" type="danger">未读</el-tag>
            </span>
            <span class="notification-content">{{ notification.content }}</span>
            <time :datetime="notification.createdAt">
              {{ formatDateTime(notification.createdAt) }}
            </time>
          </button>

          <el-button
            v-if="!notification.read"
            link
            type="primary"
            :loading="notificationStore.isMarking(notification.id)"
            :disabled="openingId !== null || notificationStore.markAllLoading"
            @click="markOne(notification, false)"
          >
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
.summary {
  color: #6b7280;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.notification-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.notification-item--unread {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.notification-main {
  display: grid;
  flex: 1;
  min-width: 0;
  gap: 6px;
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

.notification-content {
  overflow-wrap: anywhere;
  color: #4b5563;
  white-space: pre-wrap;
}

.notification-main time {
  color: #9ca3af;
  font-size: 12px;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 640px) {
  .page__header,
  .notification-item {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>

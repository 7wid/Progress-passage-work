import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getUnreadNotificationCount,
  markAllNotificationsRead as markAllNotificationsReadApi,
  markNotificationRead,
} from '@/api/notifications'
import type { MarkAllNotificationsReadResult, NotificationRecord } from '@/types/notification'

export const useNotificationStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  const unreadLoading = ref(false)
  const initialized = ref(false)
  const markingIds = ref<Set<string>>(new Set())
  const markAllLoading = ref(false)

  let generation = 0
  let countRevision = 0
  let unreadTask: Promise<number> | null = null
  let markAllTask: Promise<MarkAllNotificationsReadResult> | null = null
  const markTasks = new Map<string, Promise<NotificationRecord>>()

  const hasUnread = computed(() => unreadCount.value > 0)

  function updateMarkingId(id: string, marking: boolean): void {
    const next = new Set(markingIds.value)
    if (marking) next.add(id)
    else next.delete(id)
    markingIds.value = next
  }

  function isMarking(id: string): boolean {
    return markingIds.value.has(id)
  }

  async function refreshUnreadCount(): Promise<number> {
    if (unreadTask) return unreadTask

    const currentGeneration = generation
    const currentRevision = countRevision
    const task = (async () => {
      unreadLoading.value = true
      try {
        const result = await getUnreadNotificationCount()
        if (currentGeneration === generation && currentRevision === countRevision) {
          unreadCount.value = Math.max(0, result.unreadCount)
          initialized.value = true
        }
        return result.unreadCount
      } finally {
        if (currentGeneration === generation) unreadLoading.value = false
      }
    })()

    unreadTask = task
    try {
      return await task
    } finally {
      if (unreadTask === task) unreadTask = null
    }
  }

  async function markRead(notification: NotificationRecord): Promise<NotificationRecord> {
    if (notification.read) return notification

    const existing = markTasks.get(notification.id)
    if (existing) return existing

    const currentGeneration = generation
    updateMarkingId(notification.id, true)
    const task = (async () => {
      const updated = await markNotificationRead(notification.id)
      if (currentGeneration === generation && updated.read) {
        countRevision += 1
        unreadCount.value = Math.max(0, unreadCount.value - 1)
        initialized.value = true
      }
      return updated
    })()
    markTasks.set(notification.id, task)

    try {
      return await task
    } finally {
      if (markTasks.get(notification.id) === task) markTasks.delete(notification.id)
      if (currentGeneration === generation) updateMarkingId(notification.id, false)
    }
  }

  async function markAllRead(): Promise<MarkAllNotificationsReadResult> {
    if (markAllTask) return markAllTask

    const currentGeneration = generation
    markAllLoading.value = true
    const task = (async () => {
      const result = await markAllNotificationsReadApi()
      if (currentGeneration === generation) {
        countRevision += 1
        unreadCount.value = 0
        initialized.value = true
      }
      return result
    })()
    markAllTask = task

    try {
      return await task
    } finally {
      if (markAllTask === task) markAllTask = null
      if (currentGeneration === generation) markAllLoading.value = false
    }
  }

  function reset(): void {
    generation += 1
    countRevision += 1
    unreadCount.value = 0
    unreadLoading.value = false
    initialized.value = false
    markingIds.value = new Set()
    markAllLoading.value = false
    unreadTask = null
    markAllTask = null
    markTasks.clear()
  }

  return {
    unreadCount,
    unreadLoading,
    initialized,
    markAllLoading,
    hasUnread,
    isMarking,
    refreshUnreadCount,
    markRead,
    markAllRead,
    reset,
  }
})

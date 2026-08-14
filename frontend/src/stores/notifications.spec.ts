import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useNotificationStore } from './notifications'
import {
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notifications'
import type { NotificationRecord } from '@/types/notification'

vi.mock('@/api/notifications', () => ({
  getUnreadNotificationCount: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}))

const getUnreadMock = vi.mocked(getUnreadNotificationCount)
const markReadMock = vi.mocked(markNotificationRead)
const markAllMock = vi.mocked(markAllNotificationsRead)

function unreadNotification(): NotificationRecord {
  return {
    id: '31',
    type: 'DELIVERY_SUBMITTED',
    title: '需求等待验收',
    content: '负责人已经提交交付内容。',
    requestId: '10',
    read: false,
    readAt: null,
    createdAt: '2026-08-14T07:00:00Z',
  }
}

describe('notification store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('合并并发的未读数量请求', async () => {
    getUnreadMock.mockResolvedValue({ unreadCount: 4 })
    const store = useNotificationStore()

    await Promise.all([store.refreshUnreadCount(), store.refreshUnreadCount()])

    expect(getUnreadMock).toHaveBeenCalledTimes(1)
    expect(store.unreadCount).toBe(4)
    expect(store.initialized).toBe(true)
  })

  it('同一通知重复操作只提交一次并减少未读数量', async () => {
    const record = unreadNotification()
    const updated = { ...record, read: true, readAt: '2026-08-14T08:00:00Z' }
    getUnreadMock.mockResolvedValue({ unreadCount: 3 })
    markReadMock.mockResolvedValue(updated)
    const store = useNotificationStore()
    await store.refreshUnreadCount()

    const [first, second] = await Promise.all([store.markRead(record), store.markRead(record)])

    expect(markReadMock).toHaveBeenCalledTimes(1)
    expect(first).toEqual(updated)
    expect(second).toEqual(updated)
    expect(store.unreadCount).toBe(2)
    expect(store.isMarking('31')).toBe(false)
  })

  it('全部已读操作合并重复提交并清空角标', async () => {
    getUnreadMock.mockResolvedValue({ unreadCount: 5 })
    markAllMock.mockResolvedValue({ updatedCount: 5 })
    const store = useNotificationStore()
    await store.refreshUnreadCount()

    const results = await Promise.all([store.markAllRead(), store.markAllRead()])

    expect(markAllMock).toHaveBeenCalledTimes(1)
    expect(results).toEqual([{ updatedCount: 5 }, { updatedCount: 5 }])
    expect(store.unreadCount).toBe(0)
    expect(store.markAllLoading).toBe(false)
  })

  it('单条已读成功后不会被更早发出的未读查询覆盖', async () => {
    const record = unreadNotification()
    const updated = { ...record, read: true, readAt: '2026-08-14T08:00:00Z' }
    getUnreadMock.mockResolvedValueOnce({ unreadCount: 4 })
    markReadMock.mockResolvedValue(updated)
    const store = useNotificationStore()
    await store.refreshUnreadCount()

    let resolveStaleRequest: ((value: { unreadCount: number }) => void) | undefined
    getUnreadMock.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveStaleRequest = resolve
      }),
    )
    const staleRefresh = store.refreshUnreadCount()
    await store.markRead(record)
    expect(store.unreadCount).toBe(3)

    resolveStaleRequest?.({ unreadCount: 4 })
    await staleRefresh

    expect(store.unreadCount).toBe(3)
  })

  it('全部已读成功后不会被更早发出的未读查询覆盖', async () => {
    getUnreadMock.mockResolvedValueOnce({ unreadCount: 5 })
    markAllMock.mockResolvedValue({ updatedCount: 5 })
    const store = useNotificationStore()
    await store.refreshUnreadCount()

    let resolveStaleRequest: ((value: { unreadCount: number }) => void) | undefined
    getUnreadMock.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveStaleRequest = resolve
      }),
    )
    const staleRefresh = store.refreshUnreadCount()
    await store.markAllRead()
    expect(store.unreadCount).toBe(0)

    resolveStaleRequest?.({ unreadCount: 5 })
    await staleRefresh

    expect(store.unreadCount).toBe(0)
  })

  it('重置后忽略仍在途的旧用户未读请求', async () => {
    let resolveRequest: ((value: { unreadCount: number }) => void) | undefined
    getUnreadMock.mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve
      }),
    )
    const store = useNotificationStore()
    const pending = store.refreshUnreadCount()

    store.reset()
    resolveRequest?.({ unreadCount: 9 })
    await pending

    expect(store.unreadCount).toBe(0)
    expect(store.initialized).toBe(false)
  })
})

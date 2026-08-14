import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from './notifications'
import { http } from './http'
import type { NotificationRecord } from '@/types/notification'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)

const notification = {
  id: '31',
  type: 'DELIVERY_SUBMITTED',
  title: '需求等待验收',
  content: '负责人已经提交交付内容。',
  requestId: '10',
  read: true,
  readAt: '2026-08-14T08:00:00Z',
  createdAt: '2026-08-14T07:00:00Z',
} satisfies NotificationRecord

describe('notifications api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('按分页和未读条件读取通知', async () => {
    const page = { items: [notification], page: 2, pageSize: 20, total: 21, totalPages: 2 }
    getMock.mockResolvedValueOnce({ data: { data: page } } as never)

    await expect(getNotifications({ page: 2, pageSize: 20, unreadOnly: true })).resolves.toEqual(
      page,
    )
    expect(getMock).toHaveBeenCalledWith('/notifications', {
      params: { page: 2, pageSize: 20, unreadOnly: true },
    })
  })

  it('读取未读数量', async () => {
    getMock.mockResolvedValueOnce({ data: { data: { unreadCount: 7 } } } as never)

    await expect(getUnreadNotificationCount()).resolves.toEqual({ unreadCount: 7 })
    expect(getMock).toHaveBeenCalledWith('/notifications/unread-count')
  })

  it('获取 CSRF 后将单条通知设为已读', async () => {
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: notification } } as never)

    await expect(markNotificationRead('31')).resolves.toEqual(notification)
    expect(postMock).toHaveBeenCalledWith('/notifications/31/read')
    expect(getMock.mock.invocationCallOrder[0]).toBeLessThan(postMock.mock.invocationCallOrder[0]!)
  })

  it('非法通知编号不会发送请求', async () => {
    await expect(markNotificationRead('../31')).rejects.toThrow('通知编号格式不正确')
    expect(getMock).not.toHaveBeenCalled()
    expect(postMock).not.toHaveBeenCalled()
  })

  it('获取 CSRF 后将全部通知设为已读', async () => {
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: { updatedCount: 5 } } } as never)

    await expect(markAllNotificationsRead()).resolves.toEqual({ updatedCount: 5 })
    expect(postMock).toHaveBeenCalledWith('/notifications/read-all')
    expect(getMock.mock.invocationCallOrder[0]).toBeLessThan(postMock.mock.invocationCallOrder[0]!)
  })
})

import { http } from './http'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  MarkAllNotificationsReadResult,
  NotificationListQuery,
  NotificationRecord,
  NotificationUnreadCount,
} from '@/types/notification'

function requirePositiveId(value: string, label: string): string {
  if (!/^[1-9]\d*$/.test(value)) {
    throw new Error(`${label}格式不正确`)
  }
  return value
}

export async function getNotifications(
  query: NotificationListQuery,
): Promise<PageResponse<NotificationRecord>> {
  const response = await http.get<ApiResponse<PageResponse<NotificationRecord>>>('/notifications', {
    params: query,
  })
  return response.data.data
}

export async function getUnreadNotificationCount(): Promise<NotificationUnreadCount> {
  const response = await http.get<ApiResponse<NotificationUnreadCount>>(
    '/notifications/unread-count',
  )
  return response.data.data
}

export async function markNotificationRead(id: string): Promise<NotificationRecord> {
  const notificationId = requirePositiveId(id, '通知编号')
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<NotificationRecord>>(
    `/notifications/${encodeURIComponent(notificationId)}/read`,
  )
  return response.data.data
}

export async function markAllNotificationsRead(): Promise<MarkAllNotificationsReadResult> {
  await http.get<ApiResponse<string>>('/auth/csrf')
  const response =
    await http.post<ApiResponse<MarkAllNotificationsReadResult>>('/notifications/read-all')
  return response.data.data
}

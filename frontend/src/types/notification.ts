export interface NotificationRecord {
  id: string
  type: string
  title: string
  content: string
  requestId: string | null
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationListQuery {
  page: number
  pageSize: number
  unreadOnly: boolean
}

export interface NotificationUnreadCount {
  unreadCount: number
}

export interface MarkAllNotificationsReadResult {
  updatedCount: number
}

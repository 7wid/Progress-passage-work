import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import NotificationCenterView from './NotificationCenterView.vue'
import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notifications'
import type { NotificationRecord } from '@/types/notification'

const pushMock = vi.hoisted(() => vi.fn())

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('@/api/notifications', () => ({
  getNotifications: vi.fn(),
  getUnreadNotificationCount: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}))

const listMock = vi.mocked(getNotifications)
const unreadCountMock = vi.mocked(getUnreadNotificationCount)
const markReadMock = vi.mocked(markNotificationRead)
const markAllMock = vi.mocked(markAllNotificationsRead)

const ButtonStub = defineComponent({
  inheritAttrs: false,
  props: {
    disabled: Boolean,
    loading: Boolean,
  },
  emits: ['click'],
  setup(props, { attrs, emit, slots }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          disabled: props.disabled || props.loading,
          onClick: () => emit('click'),
        },
        slots.default?.(),
      )
  },
})

const stubs = {
  'el-button': ButtonStub,
  'el-card': { template: '<section><slot /></section>' },
  'el-tabs': { template: '<div><slot /></div>' },
  'el-tab-pane': true,
  'el-alert': {
    props: ['title'],
    template: '<div>{{ title }}<slot /></div>',
  },
  'el-empty': true,
  'el-tag': { template: '<span><slot /></span>' },
  'el-pagination': true,
}

function record(overrides: Partial<NotificationRecord> = {}): NotificationRecord {
  return {
    id: '31',
    type: 'DELIVERY_SUBMITTED',
    title: '需求等待验收',
    content: '负责人已经提交交付内容。',
    requestId: '10',
    read: false,
    readAt: null,
    createdAt: '2026-08-14T07:00:00Z',
    ...overrides,
  }
}

function page(items: NotificationRecord[]) {
  return { items, page: 1, pageSize: 20, total: items.length, totalPages: 1 }
}

function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return shallowMount(NotificationCenterView, {
    global: {
      plugins: [pinia],
      stubs,
      directives: { loading: () => undefined },
    },
  })
}

describe('NotificationCenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    unreadCountMock.mockResolvedValue({ unreadCount: 1 })
  })

  it('加载通知并始终以纯文本显示服务端内容', async () => {
    const unsafe = record({
      title: '<script>alert(1)</script>',
      content: '<img src=x onerror=alert(1)>',
    })
    listMock.mockResolvedValue(page([unsafe]))
    const wrapper = mountView()
    await flushPromises()

    expect(listMock).toHaveBeenCalledWith({ page: 1, pageSize: 20, unreadOnly: false })
    expect(wrapper.text()).toContain('<script>alert(1)</script>')
    expect(wrapper.text()).toContain('<img src=x onerror=alert(1)>')
    expect(wrapper.find('script').exists()).toBe(false)
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('点击关联需求的未读通知时先标已读再跳转详情', async () => {
    const unread = record()
    const updated = { ...unread, read: true, readAt: '2026-08-14T08:00:00Z' }
    listMock.mockResolvedValue(page([unread]))
    markReadMock.mockResolvedValue(updated)
    pushMock.mockResolvedValue(undefined)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-notification-id="31"] .notification-main').trigger('click')
    await flushPromises()

    expect(markReadMock).toHaveBeenCalledWith('31')
    expect(pushMock).toHaveBeenCalledWith({ name: 'request-detail', params: { id: '10' } })
    expect(markReadMock.mock.invocationCallOrder[0]).toBeLessThan(
      pushMock.mock.invocationCallOrder[0]!,
    )
  })

  it('无关联需求的通知只标为已读，不执行跳转', async () => {
    const unread = record({ requestId: null })
    markReadMock.mockResolvedValue({ ...unread, read: true, readAt: '2026-08-14T08:00:00Z' })
    listMock.mockResolvedValue(page([unread]))
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-notification-id="31"] .notification-main').trigger('click')
    await flushPromises()

    expect(markReadMock).toHaveBeenCalledWith('31')
    expect(pushMock).not.toHaveBeenCalled()
    expect(listMock).toHaveBeenCalledTimes(2)
  })

  it('全部已读成功后重新加载第一页', async () => {
    listMock.mockResolvedValue(page([record()]))
    markAllMock.mockResolvedValue({ updatedCount: 1 })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-test="mark-all"]').trigger('click')
    await flushPromises()

    expect(markAllMock).toHaveBeenCalledTimes(1)
    expect(listMock).toHaveBeenCalledTimes(2)
  })

  it('列表加载失败时显示可重试错误', async () => {
    listMock.mockRejectedValue({
      isAxiosError: true,
      response: { data: { error: { message: '通知服务暂时不可用' } } },
    })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('通知服务暂时不可用')
    expect(wrapper.text()).toContain('重新加载')
  })
})

import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import NotificationBell from './NotificationBell.vue'
import { getUnreadNotificationCount } from '@/api/notifications'

vi.mock('@/api/notifications', () => ({
  getUnreadNotificationCount: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}))

const getUnreadMock = vi.mocked(getUnreadNotificationCount)

function mountBell() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return shallowMount(NotificationBell, {
    global: {
      plugins: [pinia],
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a><slot /></a>',
        },
        'el-badge': {
          props: ['value', 'max', 'hidden'],
          template: '<span><slot /></span>',
        },
      },
    },
  })
}

describe('NotificationBell', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      value: 'visible',
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('首次加载、每 60 秒和页面恢复可见时刷新未读数，卸载后停止', async () => {
    getUnreadMock.mockResolvedValue({ unreadCount: 3 })
    const wrapper = mountBell()
    await flushPromises()

    expect(getUnreadMock).toHaveBeenCalledTimes(1)
    expect(wrapper.get('a').attributes('aria-label')).toBe('通知，3 条未读')

    await vi.advanceTimersByTimeAsync(60_000)
    expect(getUnreadMock).toHaveBeenCalledTimes(2)

    document.dispatchEvent(new Event('visibilitychange'))
    await flushPromises()
    expect(getUnreadMock).toHaveBeenCalledTimes(3)

    wrapper.unmount()
    await vi.advanceTimersByTimeAsync(120_000)
    document.dispatchEvent(new Event('visibilitychange'))
    await flushPromises()
    expect(getUnreadMock).toHaveBeenCalledTimes(3)
  })

  it('页面隐藏时轮询不会发送请求', async () => {
    getUnreadMock.mockResolvedValue({ unreadCount: 0 })
    const wrapper = mountBell()
    await flushPromises()
    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      value: 'hidden',
    })

    await vi.advanceTimersByTimeAsync(60_000)

    expect(getUnreadMock).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })
})

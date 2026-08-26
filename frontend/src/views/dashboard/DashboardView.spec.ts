import { defineComponent, h, inject, provide } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardView from './DashboardView.vue'
import { getRequests } from '@/api/requests'
import RequesterActionCenter from '@/components/requests/RequesterActionCenter.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/requests', () => ({ getRequests: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))

const getRequestsMock = vi.mocked(getRequests)
const tableRowsKey = 'dashboard-table-rows'
const ButtonStub = defineComponent({
  props: { loading: Boolean },
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () => h('button', { onClick: () => emit('click') }, slots.default?.())
  },
})
const TableStub = defineComponent({
  props: { data: { type: Array, default: () => [] } },
  setup(props, { slots }) {
    provide(tableRowsKey, props)
    return () => h('div', { 'data-testid': 'dashboard-table' }, slots.default?.())
  },
})
const TableColumnStub = defineComponent({
  setup(_props, { slots }) {
    const table = inject<{ data: Array<Record<string, unknown>> }>(tableRowsKey, { data: [] })
    return () =>
      h(
        'div',
        table.data.map((row) => slots.default?.({ row })),
      )
  },
})

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.user = {
      id: '1',
      account: 'requester',
      displayName: '需求方',
      role: 'REQUESTER',
    }
    getRequestsMock.mockImplementation(async (query) => {
      const actionTitles: Partial<Record<string, string>> = {
        PENDING_ACCEPTANCE: '确认交付成果',
        NEED_MORE_INFO: '补充活动资料',
        DRAFT: '完善需求草稿',
      }
      const actionTitle = query.status ? actionTitles[query.status] : undefined
      return {
        items: actionTitle
          ? [
              {
                id: `request-${query.status}`,
                requestNo: query.status === 'DRAFT' ? null : `REQ-${query.status}`,
                title: actionTitle,
                categoryId: 'category-1',
                categoryName: '网站开发',
                creatorName: '需求方',
                urgency: 'NORMAL',
                status: query.status!,
                progress: 0,
                expectedDeadline: '2026-08-30',
                submittedAt: query.status === 'DRAFT' ? null : '2026-08-19T08:00:00Z',
                createdAt: '2026-08-19T08:00:00Z',
              },
            ]
          : query.status
            ? []
            : [
                {
                  id: 'request-1',
                  requestNo: 'REQ-20260819-0001',
                  title: '社团报名网站开发',
                  categoryId: 'category-1',
                  categoryName: '网站开发',
                  creatorName: '需求方',
                  urgency: 'NORMAL',
                  status: 'PENDING_REVIEW',
                  progress: 0,
                  expectedDeadline: '2026-08-30',
                  submittedAt: '2026-08-19T08:00:00Z',
                  createdAt: '2026-08-19T08:00:00Z',
                },
              ],
        page: query.page,
        pageSize: query.pageSize,
        total: query.status === 'COMPLETED' ? 3 : query.status ? 1 : 8,
        totalPages: 1,
      }
    })
  })

  it('加载需求方总数、最近需求和四个状态计数', async () => {
    const wrapper = shallowMount(DashboardView, {
      global: {
        directives: { loading: () => undefined },
        stubs: {
          'el-alert': true,
          'el-button': ButtonStub,
          'el-card': { template: '<section><slot /></section>' },
          'el-table': TableStub,
          'el-table-column': TableColumnStub,
        },
      },
    })
    await flushPromises()

    expect(getRequestsMock).toHaveBeenCalledTimes(7)
    expect(wrapper.text()).toContain('共 8 条需求')
    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('待我处理')

    const actionCenter = wrapper.findComponent(RequesterActionCenter)
    expect(actionCenter.props('total')).toBe(3)
    expect(
      (actionCenter.props('items') as Array<{ status: string }>).map((item) => item.status),
    ).toEqual(['PENDING_ACCEPTANCE', 'NEED_MORE_INFO', 'DRAFT'])
  })

  it('为最近需求标题提供键盘可聚焦的详情入口', async () => {
    const wrapper = shallowMount(DashboardView, {
      global: {
        directives: { loading: () => undefined },
        stubs: {
          'el-alert': true,
          'el-button': ButtonStub,
          'el-card': { template: '<section><slot /></section>' },
          'el-table': TableStub,
          'el-table-column': TableColumnStub,
        },
      },
    })
    await flushPromises()

    const titleLink = wrapper.find('button.request-title-link')
    expect(titleLink.exists()).toBe(true)
    expect(titleLink.text()).toBe('社团报名网站开发')
  })
})

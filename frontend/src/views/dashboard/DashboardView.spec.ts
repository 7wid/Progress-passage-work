import { defineComponent, h } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardView from './DashboardView.vue'
import { getRequests } from '@/api/requests'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/requests', () => ({ getRequests: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))

const getRequestsMock = vi.mocked(getRequests)
const ButtonStub = defineComponent({
  props: { loading: Boolean },
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () => h('button', { onClick: () => emit('click') }, slots.default?.())
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
    getRequestsMock.mockImplementation(async (query) => ({
      items: [],
      page: query.page,
      pageSize: query.pageSize,
      total: query.status === 'COMPLETED' ? 3 : query.status ? 1 : 8,
      totalPages: 1,
    }))
  })

  it('加载需求方总数、最近需求和四个状态计数', async () => {
    const wrapper = shallowMount(DashboardView, {
      global: {
        directives: { loading: () => undefined },
        stubs: {
          'el-alert': true,
          'el-button': ButtonStub,
          'el-card': { template: '<section><slot /></section>' },
          'el-table': true,
          'el-table-column': true,
        },
      },
    })
    await flushPromises()

    expect(getRequestsMock).toHaveBeenCalledTimes(5)
    expect(wrapper.text()).toContain('共 8 条需求')
    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.text()).toContain('3')
  })
})

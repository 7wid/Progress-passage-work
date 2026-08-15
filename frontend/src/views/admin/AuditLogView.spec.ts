import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditLogView from './AuditLogView.vue'
import { getAdminAuditLogs } from '@/api/auditLogs'

vi.mock('@/api/auditLogs', () => ({ getAdminAuditLogs: vi.fn() }))

const listMock = vi.mocked(getAdminAuditLogs)

const ButtonStub = defineComponent({
  props: { loading: Boolean },
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () =>
      h('button', { disabled: props.loading, onClick: () => emit('click') }, slots.default?.())
  },
})

function mountView() {
  return shallowMount(AuditLogView, {
    global: {
      directives: { loading: () => undefined },
      stubs: {
        'el-alert': { template: '<aside><slot /></aside>' },
        'el-button': ButtonStub,
        'el-card': { template: '<section><slot /></section>' },
        'el-date-picker': true,
        'el-descriptions': true,
        'el-descriptions-item': true,
        'el-dialog': true,
        'el-empty': true,
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<label><slot /></label>' },
        'el-input': true,
        'el-option': true,
        'el-pagination': true,
        'el-select': true,
        'el-table': true,
        'el-table-column': true,
        'el-tag': true,
      },
    },
  })
}

describe('AuditLogView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listMock.mockResolvedValue({ items: [], page: 1, pageSize: 20, total: 0, totalPages: 0 })
  })

  it('进入页面按本月范围加载只读审计记录', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(listMock).toHaveBeenCalledTimes(1)
    const query = listMock.mock.calls[0]?.[0]
    expect(query?.page).toBe(1)
    expect(query?.pageSize).toBe(20)
    expect(query?.from).toMatch(/^\d{4}-\d{2}-01$/)
    expect(wrapper.text()).toContain('关键操作只读追踪')
    expect(wrapper.text()).not.toContain('删除审计')
    expect(wrapper.text()).not.toContain('编辑审计')
  })

  it('加载失败保留页面和重新加载入口', async () => {
    listMock.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('重新加载')
    expect(wrapper.text()).toContain('审计记录')
  })
})

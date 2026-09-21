import { defineComponent, h } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { enableAutoUnmount, flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import RequestListView from './RequestListView.vue'
import { getRequests } from '@/api/requests'
import type { PageResponse } from '@/types/api'
import type { RequestSummary } from '@/types/request'

vi.mock('@/api/requests', () => ({ getRequests: vi.fn() }))
vi.mock('@/api/categories', () => ({ getEnabledCategories: vi.fn().mockResolvedValue([]) }))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
}))
const response = (total: number): PageResponse<RequestSummary> => ({
  items: [],
  page: 1,
  pageSize: 20,
  total,
  totalPages: 1,
})
const InputStub = defineComponent({
  props: ['modelValue'],
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () =>
      h('input', {
        value: props.modelValue,
        onInput: (event: Event) =>
          emit('update:modelValue', (event.target as HTMLInputElement).value),
      })
  },
})
function mountView() {
  return shallowMount(RequestListView, {
    global: {
      directives: { loading: () => undefined },
      stubs: {
        'el-card': { template: '<section><slot name="header" /><slot /></section>' },
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-button': { template: '<button><slot /></button>' },
        'el-input': InputStub,
        'el-select': true,
        'el-option': true,
        'el-date-picker': true,
        'el-table': true,
        'el-table-column': true,
        'el-tag': true,
        'el-progress': true,
        'el-pagination': true,
        'el-alert': { props: ['title'], template: '<aside>{{ title }}<slot /></aside>' },
      },
    },
  })
}
enableAutoUnmount(afterEach)
beforeEach(() => {
  vi.clearAllMocks()
  setActivePinia(createPinia())
  vi.mocked(getRequests).mockResolvedValue(response(0))
})
describe('需求筛选体验', () => {
  it('失败后保留输入并可重试；摘要只代表已返回的查询', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('input').setValue('报名')
    expect(wrapper.get('.result-summary').text()).not.toContain('报名')
    vi.mocked(getRequests).mockRejectedValueOnce(new Error('offline'))
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('input').element.value).toBe('报名')
    expect(wrapper.text()).toContain('筛选条件已保留')
    expect(wrapper.find('.collection-empty').exists()).toBe(false)
    await wrapper
      .findAll('button')
      .find((button) => button.text() === '重新加载')!
      .trigger('click')
    await flushPromises()
    expect(wrapper.get('.result-summary').text()).toContain('关键词：报名')
  })
  it('较早的请求不能覆盖后发查询', async () => {
    let finish!: (result: PageResponse<RequestSummary>) => void
    vi.mocked(getRequests).mockReturnValueOnce(
      new Promise((resolve) => {
        finish = resolve
      }),
    )
    const wrapper = mountView()
    await wrapper.get('input').setValue('新查询')
    vi.mocked(getRequests).mockResolvedValueOnce(response(2))
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    finish(response(9))
    await flushPromises()
    expect(wrapper.get('.results-heading').text()).toContain('共 2 条')
    expect(wrapper.get('.result-summary').text()).toContain('新查询')
  })
})

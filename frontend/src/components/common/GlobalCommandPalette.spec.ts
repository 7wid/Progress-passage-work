import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import GlobalCommandPalette from './GlobalCommandPalette.vue'

const { pushMock } = vi.hoisted(() => ({ pushMock: vi.fn() }))

vi.mock('vue-router', () => ({ useRouter: () => ({ push: pushMock }) }))

const DialogStub = defineComponent({
  props: { modelValue: Boolean },
  emits: ['update:modelValue', 'opened'],
  setup(props, { slots }) {
    return () =>
      props.modelValue ? h('section', [slots.header?.(), slots.default?.()]) : undefined
  },
})

const TestIcon = defineComponent({
  setup() {
    return () => h('span')
  },
})

function mountPalette() {
  return shallowMount(GlobalCommandPalette, {
    props: {
      modelValue: true,
      canCreateRequest: true,
      navigationItems: [
        { label: '概览', group: '工作区', to: '/dashboard', icon: TestIcon },
        { label: '年度数据报表', group: '组织管理', to: '/admin/statistics', icon: TestIcon },
      ],
    },
    global: {
      stubs: {
        'el-dialog': DialogStub,
      },
    },
  })
}

describe('GlobalCommandPalette', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('输入关键词后可用方向键选择匹配页面并跳转', async () => {
    const wrapper = mountPalette()
    const input = wrapper.get('input')

    await input.setValue('年度')
    await input.trigger('keydown', { key: 'ArrowDown' })
    await input.trigger('keydown', { key: 'Enter' })
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith('/admin/statistics')
    expect(wrapper.emitted('update:modelValue')).toContainEqual([false])
  })

  it('把任意关键词带入需求列表检索', async () => {
    const wrapper = mountPalette()
    await wrapper.get('input').setValue('小程序改版')
    await wrapper.get('input').trigger('keydown', { key: 'Enter' })
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith({
      name: 'request-list',
      query: { keyword: '小程序改版' },
    })
  })
})

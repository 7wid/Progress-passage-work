import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RegisterView from './RegisterView.vue'
import { getRegistrationStatus } from '@/api/auth'

const replaceMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: replaceMock, push: vi.fn() }),
}))
vi.mock('@/api/auth', () => ({
  getRegistrationStatus: vi.fn(),
  register: vi.fn(),
}))

const getRegistrationStatusMock = vi.mocked(getRegistrationStatus)
const ResultStub = defineComponent({
  props: { title: String, subTitle: String },
  setup(props, { slots }) {
    return () => h('section', [h('h2', props.title), h('p', props.subTitle), slots.extra?.()])
  },
})
const ButtonStub = defineComponent({
  emits: ['click'],
  setup(_props, { emit, slots }) {
    return () => h('button', { onClick: () => emit('click') }, slots.default?.())
  },
})

describe('RegisterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getRegistrationStatusMock.mockResolvedValue({ enabled: false, emailSuffix: null })
  })

  it('关闭自助注册时说明账号获取路径和密码安全边界', async () => {
    const wrapper = shallowMount(RegisterView, {
      global: {
        directives: { loading: () => undefined },
        stubs: {
          AuthLayout: { template: '<main><slot /></main>' },
          'el-result': ResultStub,
          'el-button': ButtonStub,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('当前采用受控开通方式')
    expect(wrapper.text()).toContain('联系管理员')
    expect(wrapper.text()).toContain('不要通过聊天发送个人密码')

    const buttons = wrapper.findAll('button')
    await buttons[0]?.trigger('click')
    await buttons[1]?.trigger('click')
    expect(replaceMock).toHaveBeenNthCalledWith(1, '/')
    expect(replaceMock).toHaveBeenNthCalledWith(2, '/login')
  })
})

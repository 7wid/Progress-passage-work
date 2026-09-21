import { defineComponent, h } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { enableAutoUnmount, flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import SettingsView from './SettingsView.vue'
import { getProfile, updateProfile } from '@/api/profile'

const hooks = vi.hoisted(() => ({
  leave: undefined as (() => Promise<boolean>) | undefined,
  confirm: vi.fn(),
}))
vi.mock('vue-router', () => ({
  onBeforeRouteLeave: (guard: () => Promise<boolean>) => {
    hooks.leave = guard
  },
}))
vi.mock('@/api/profile', () => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  changePassword: vi.fn(),
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), info: vi.fn() },
  ElMessageBox: { confirm: hooks.confirm },
}))
const validate = vi.fn<() => Promise<boolean>>()
const profile = {
  id: '1',
  account: 'requester',
  displayName: '申请人',
  email: 'demo@example.com',
  phone: '',
  department: '技术组',
  role: 'REQUESTER' as const,
}
const FormStub = defineComponent({
  props: { disabled: Boolean },
  emits: ['submit'],
  setup(props, { slots, emit, expose }) {
    expose({ validate, resetFields: vi.fn() })
    return () =>
      h(
        'form',
        {
          onSubmit: (event: Event) => {
            event.preventDefault()
            emit('submit', event)
          },
        },
        h('fieldset', { disabled: props.disabled }, slots.default?.()),
      )
  },
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
  return shallowMount(SettingsView, {
    global: {
      directives: { loading: () => undefined },
      stubs: {
        'el-card': { template: '<section><slot name="header" /><slot /></section>' },
        'el-form': FormStub,
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': InputStub,
        'el-tag': true,
        'el-button': { template: '<button><slot /></button>' },
      },
    },
  })
}
enableAutoUnmount(afterEach)
beforeEach(() => {
  vi.clearAllMocks()
  setActivePinia(createPinia())
  validate.mockResolvedValue(true)
  vi.mocked(getProfile).mockResolvedValue(profile)
  vi.mocked(updateProfile).mockResolvedValue({ ...profile, displayName: '新称呼' })
})
describe('个人设置保存反馈', () => {
  it('从异步校验开始阻止重复保存，并使用提交快照', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('input').setValue('新称呼')
    let finish!: (valid: boolean) => void
    validate.mockReturnValueOnce(
      new Promise((resolve) => {
        finish = resolve
      }),
    )
    await wrapper.findAll('form')[0]!.trigger('submit')
    await wrapper.findAll('form')[0]!.trigger('submit')
    expect(validate).toHaveBeenCalledTimes(1)
    expect(updateProfile).not.toHaveBeenCalled()
    finish(true)
    await flushPromises()
    expect(updateProfile).toHaveBeenCalledTimes(1)
    expect(updateProfile).toHaveBeenCalledWith(expect.objectContaining({ displayName: '新称呼' }))
    expect(wrapper.text()).toContain('资料已保存')
    expect(wrapper.find('.settings-save-hint').exists()).toBe(false)
  })
  it('请求失败保留输入与错误，重试成功后清除错误', async () => {
    vi.mocked(updateProfile).mockRejectedValueOnce(new Error('offline'))
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('input').setValue('新称呼')
    await wrapper.findAll('form')[0]!.trigger('submit')
    await flushPromises()
    expect(wrapper.get('input').element.value).toBe('新称呼')
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    await wrapper.findAll('form')[0]!.trigger('submit')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })
  it('未保存修改可取消离页，保存后不再提示', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(await hooks.leave!()).toBe(true)
    await wrapper.get('input').setValue('新称呼')
    hooks.confirm.mockRejectedValueOnce('cancel')
    expect(await hooks.leave!()).toBe(false)
    expect(wrapper.get('input').element.value).toBe('新称呼')
    await wrapper.findAll('form')[0]!.trigger('submit')
    await flushPromises()
    expect(await hooks.leave!()).toBe(true)
    expect(hooks.confirm).toHaveBeenCalledTimes(1)
  })
})

import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AdminReasonDialog from './AdminReasonDialog.vue'

const DialogStub = defineComponent({
  props: { modelValue: Boolean },
  setup(props, { slots }) {
    return () =>
      props.modelValue
        ? h('section', [slots.default?.(), h('footer', slots.footer?.())])
        : undefined
  },
})

const FormStub = defineComponent({
  setup(_props, { expose, slots }) {
    expose({ validate: async () => true, clearValidate: () => undefined })
    return () => h('form', slots.default?.())
  },
})

const InputStub = defineComponent({
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () =>
      h('textarea', {
        value: props.modelValue,
        onInput: (event: Event) =>
          emit('update:modelValue', (event.target as HTMLTextAreaElement).value),
      })
  },
})

const ButtonStub = defineComponent({
  props: { disabled: Boolean, loading: Boolean },
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () =>
      h(
        'button',
        {
          disabled: props.disabled || props.loading,
          onClick: () => emit('click'),
        },
        slots.default?.(),
      )
  },
})

describe('AdminReasonDialog', () => {
  it('校验通过后只提交清理过的操作原因', async () => {
    const wrapper = shallowMount(AdminReasonDialog, {
      props: {
        modelValue: true,
        title: '停用成员',
        description: '停用后会使登录会话失效。',
        confirmText: '确认停用',
      },
      global: {
        stubs: {
          'el-dialog': DialogStub,
          'el-alert': true,
          'el-form': FormStub,
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': InputStub,
          'el-button': ButtonStub,
        },
      },
    })

    await wrapper.get('textarea').setValue('  成员已经退出技术组  ')
    await wrapper.findAll('button')[1]!.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('confirm')).toEqual([['成员已经退出技术组']])
  })

  it('提交中不能关闭或重复确认', async () => {
    const wrapper = shallowMount(AdminReasonDialog, {
      props: {
        modelValue: true,
        title: '停用成员',
        description: '停用后会使登录会话失效。',
        confirmText: '确认停用',
        submitting: true,
      },
      global: {
        stubs: {
          'el-dialog': DialogStub,
          'el-alert': true,
          'el-form': FormStub,
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': InputStub,
          'el-button': ButtonStub,
        },
      },
    })

    await wrapper.findAll('button')[0]!.trigger('click')
    await wrapper.findAll('button')[1]!.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
    expect(wrapper.emitted('confirm')).toBeUndefined()
  })
})

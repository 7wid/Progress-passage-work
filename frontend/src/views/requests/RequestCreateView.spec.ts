import { defineComponent, h } from 'vue'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import RequestCreateView from './RequestCreateView.vue'
import { getEnabledCategories } from '@/api/categories'
import {
  createDraft,
  createRequest,
  getRequestDetail,
  submitRequest,
  updateRequest,
} from '@/api/requests'
import type { RequestDetail, RequestMutation } from '@/types/request'

vi.mock('@/api/categories', () => ({ getEnabledCategories: vi.fn() }))
vi.mock('@/api/requests', () => ({
  createDraft: vi.fn(),
  createRequest: vi.fn(),
  getRequestDetail: vi.fn(),
  submitRequest: vi.fn(),
  updateRequest: vi.fn(),
}))

const validate = vi.fn<() => Promise<boolean>>()
// Element Plus types combine the confirm action with prompt input data; confirm resolves an action.
const confirmed = 'confirm' as Awaited<ReturnType<typeof ElMessageBox.confirm>>
const FormStub = defineComponent({
  props: { disabled: Boolean },
  emits: ['submit'],
  setup(props, { emit, expose, slots }) {
    expose({ validate, clearValidate: vi.fn() })
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
  props: { modelValue: [String, Number] },
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
const ButtonStub = defineComponent({
  props: { disabled: Boolean, loading: Boolean, nativeType: String },
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () =>
      h(
        'button',
        {
          type: props.nativeType ?? 'button',
          disabled: props.disabled || props.loading,
          onClick: () => emit('click'),
        },
        slots.default?.(),
      )
  },
})

function detail(overrides: Partial<RequestDetail> = {}): RequestDetail {
  return {
    id: '101',
    requestNo: null,
    title: '测试需求草稿',
    categoryId: '1',
    categoryName: '应用开发',
    creatorId: '20',
    creatorName: '需求方',
    status: 'DRAFT',
    progress: 0,
    urgency: 'NORMAL',
    expectedDeadline: '2099-12-01',
    submittedAt: null,
    createdAt: '2026-08-28T08:00:00Z',
    updatedAt: '2026-08-28T08:00:00Z',
    version: 3,
    background: '测试需求背景',
    description: '测试需求说明',
    expectedResult: '测试期望成果',
    budgetAmount: null,
    budgetDescription: null,
    technicalConstraints: null,
    contactInfo: 'test@example.com',
    statusHistory: [],
    ...overrides,
  }
}

function mutation(version: number, status: RequestMutation['status'] = 'DRAFT'): RequestMutation {
  return { id: '101', requestNo: 'REQ-101', version, status }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason: unknown) => void
  const promise = new Promise<T>((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}

async function mountPage(path = '/requests/101/edit') {
  const Placeholder = defineComponent({ render: () => h('p', '其他页面') })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/requests/new', name: 'request-create', component: RequestCreateView },
      { path: '/requests/:id/edit', name: 'request-edit', component: RequestCreateView },
      { path: '/requests/:id', name: 'request-detail', component: Placeholder },
      { path: '/requests', component: Placeholder },
    ],
  })
  await router.push(path)
  await router.isReady()
  // Match the app shell: route changes remount the editor, including query changes.
  const wrapper = mount(
    defineComponent({
      components: { RouterView },
      template:
        '<RouterView v-slot="{ Component, route }"><div :key="route.fullPath"><component :is="Component" /></div></RouterView>',
    }),
    {
      global: {
        plugins: [router],
        directives: { loading: () => undefined },
        stubs: {
          AppPageHeader: true,
          RequestSupplementGuide: true,
          'el-card': { template: '<section><slot /></section>' },
          'el-form': FormStub,
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': InputStub,
          'el-button': ButtonStub,
          'el-select': true,
          'el-option': true,
          'el-date-picker': true,
          'el-radio-group': true,
          'el-radio': true,
          'el-checkbox': true,
        },
      },
    },
  )
  await flushPromises()
  return { wrapper, router }
}

enableAutoUnmount(afterEach)
afterEach(() => vi.restoreAllMocks())

describe('需求编辑与提交可靠性', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    validate.mockResolvedValue(true)
    vi.mocked(getEnabledCategories).mockResolvedValue([{ id: '1', name: '应用开发' }])
    vi.mocked(getRequestDetail).mockResolvedValue(detail())
    vi.mocked(updateRequest).mockResolvedValue(mutation(4))
    vi.mocked(submitRequest).mockResolvedValue(mutation(5, 'PENDING_REVIEW'))
    vi.spyOn(ElMessage, 'success').mockReturnValue({ close: vi.fn() })
    vi.spyOn(ElMessage, 'warning').mockReturnValue({ close: vi.fn() })
    vi.spyOn(ElMessage, 'error').mockReturnValue({ close: vi.fn() })
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue(confirmed)
  })

  it('保存成功但提交失败后保留输入，并使用最新版本重试', async () => {
    vi.mocked(submitRequest).mockRejectedValueOnce(new Error('offline'))
    const { wrapper, router } = await mountPage()
    await wrapper.get('input').setValue('补充后的需求标题')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('input').element.value).toBe('补充后的需求标题')
    expect(wrapper.text()).toContain('内容已保存')
    expect(router.currentRoute.value.path).toBe('/requests/101/edit')

    vi.mocked(updateRequest).mockResolvedValueOnce(mutation(5))
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(updateRequest).toHaveBeenNthCalledWith(
      2,
      '101',
      expect.objectContaining({ title: '补充后的需求标题' }),
      4,
    )
    expect(submitRequest).toHaveBeenLastCalledWith('101', 5)
    expect(router.currentRoute.value.path).toBe('/requests/101')
  })

  it('校验尚未结束时阻止连续提交及保存', async () => {
    const validation = deferred<boolean>()
    validate.mockReturnValue(validation.promise)
    const { wrapper } = await mountPage()
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')
    const saveButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('保存修改'))!
    await saveButton.trigger('click')
    expect(validate).toHaveBeenCalledTimes(1)
    expect(wrapper.get('fieldset').element.disabled).toBe(true)
    expect(updateRequest).not.toHaveBeenCalled()
    validation.resolve(true)
    await flushPromises()
    expect(updateRequest).toHaveBeenCalledTimes(1)
    expect(submitRequest).toHaveBeenCalledTimes(1)
  })

  it('409 冲突保留输入、显示详情入口并阻止继续覆盖', async () => {
    vi.mocked(updateRequest).mockRejectedValue({ isAxiosError: true, response: { status: 409 } })
    const { wrapper } = await mountPage()
    await wrapper.get('input').setValue('保留这段尚未提交的修改')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('已变化')
    expect(wrapper.get('a[href="/requests/101"]').text()).toContain('查看最新详情')
    expect(wrapper.get('input').element.value).toBe('保留这段尚未提交的修改')
    await wrapper.get('form').trigger('submit')
    expect(updateRequest).toHaveBeenCalledTimes(1)
    expect(submitRequest).not.toHaveBeenCalled()
  })

  it('校验不通过时不写入接口并恢复编辑', async () => {
    validate.mockResolvedValue(false)
    const { wrapper } = await mountPage()
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(updateRequest).not.toHaveBeenCalled()
    expect(submitRequest).not.toHaveBeenCalled()
    expect(wrapper.get('fieldset').element.disabled).toBe(false)
  })

  it('新需求使用创建接口；未完成的表单也可单独保存草稿', async () => {
    vi.mocked(createDraft).mockResolvedValue({ id: '102', requestNo: null, status: 'DRAFT' })
    const { wrapper, router } = await mountPage('/requests/new')
    await wrapper.get('input').setValue('草稿')
    await wrapper
      .findAll('button')
      .find((button) => button.text().includes('保存草稿'))!
      .trigger('click')
    await flushPromises()
    expect(validate).not.toHaveBeenCalled()
    expect(createDraft).toHaveBeenCalledWith(expect.objectContaining({ title: '草稿' }))
    expect(createRequest).not.toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/requests/102')
  })

  it('新需求校验通过后直接创建并进入详情', async () => {
    vi.mocked(createRequest).mockResolvedValue({
      id: '102',
      requestNo: 'REQ-102',
      status: 'PENDING_REVIEW',
    })
    const { wrapper, router } = await mountPage('/requests/new')
    await wrapper.get('input').setValue('新需求标题')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(createRequest).toHaveBeenCalledWith(expect.objectContaining({ title: '新需求标题' }))
    expect(updateRequest).not.toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/requests/102')
  })

  it('补充资料才展示补充要求入口，提交后返回原需求', async () => {
    vi.mocked(getRequestDetail).mockResolvedValue(
      detail({ status: 'NEED_MORE_INFO', requestNo: 'REQ-101' }),
    )
    const { wrapper, router } = await mountPage()
    expect(wrapper.get('request-supplement-guide-stub').attributes('requestid')).toBe('101')
    expect(wrapper.text()).toContain('提交补充资料')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(submitRequest).toHaveBeenCalledWith('101', 4)
    expect(ElMessage.success).toHaveBeenCalledWith('需求 REQ-101 的补充资料已提交')
    expect(router.currentRoute.value.path).toBe('/requests/101')
  })

  it('保存失败显示字段错误、不执行提交且不丢失输入', async () => {
    vi.mocked(updateRequest).mockRejectedValueOnce({
      isAxiosError: true,
      response: {
        status: 400,
        data: {
          error: {
            message: '日期校验失败',
            details: [{ field: 'expectedDeadline', message: '日期不能早于今天' }],
          },
        },
      },
    })
    const { wrapper } = await mountPage()
    expect(wrapper.find('request-supplement-guide-stub').exists()).toBe(false)
    await wrapper.get('input').setValue('需要保留的内容')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('日期校验失败')
    expect(wrapper.get('[prop="expectedDeadline"]').attributes('error')).toBe('日期不能早于今天')
    expect(wrapper.get('input').element.value).toBe('需要保留的内容')
    expect(submitRequest).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('本次填写内容已保存')
  })

  it('手动保存只保存内容、不提交，并更新后续提交的版本', async () => {
    const { wrapper } = await mountPage()
    await wrapper
      .findAll('button')
      .find((button) => button.text().includes('保存修改'))!
      .trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('内容已保存，尚未提交')
    expect(submitRequest).not.toHaveBeenCalled()
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(updateRequest).toHaveBeenNthCalledWith(2, '101', expect.any(Object), 4)
  })

  it('保存期间禁用表单，阻止跳到其他需求，并提示关闭页面的风险', async () => {
    const pending = deferred<RequestMutation>()
    vi.mocked(updateRequest).mockReturnValue(pending.promise)
    const { wrapper, router } = await mountPage()
    await wrapper
      .findAll('button')
      .find((button) => button.text().includes('保存修改'))!
      .trigger('click')
    await flushPromises()
    expect(wrapper.get('fieldset').element.disabled).toBe(true)
    await router.push('/requests/202/edit')
    expect(router.currentRoute.value.path).toBe('/requests/101/edit')
    await router.push('/requests')
    expect(router.currentRoute.value.path).toBe('/requests/101/edit')
    const event = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(event)
    expect(event.defaultPrevented).toBe(true)
    pending.resolve(mutation(4))
    await flushPromises()
    expect(wrapper.get('fieldset').element.disabled).toBe(false)
    const afterSave = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(afterSave)
    expect(afterSave.defaultPrevented).toBe(false)
  })

  it('切换编辑目标或查询参数前确认未保存内容，取消后留在原表单', async () => {
    const { wrapper, router } = await mountPage()
    await wrapper.get('input').setValue('不要丢掉这段输入')
    vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')
    await router.push('/requests/202/edit')
    await router.push('/requests/101/edit?source=notification')
    expect(router.currentRoute.value.fullPath).toBe('/requests/101/edit')
    expect(wrapper.get('input').element.value).toBe('不要丢掉这段输入')
    expect(ElMessageBox.confirm).toHaveBeenCalledTimes(2)
    vi.mocked(ElMessageBox.confirm).mockResolvedValue(confirmed)
    vi.mocked(getRequestDetail).mockResolvedValue(detail({ id: '202', title: '另一个需求' }))
    await router.push('/requests/202/edit')
    await flushPromises()
    expect(getRequestDetail).toHaveBeenLastCalledWith('202')
    expect(wrapper.get('input').element.value).toBe('另一个需求')
  })

  it('分类加载失败可重试且不清空表单；恢复前禁止提交', async () => {
    vi.mocked(getEnabledCategories).mockRejectedValueOnce(new Error('offline'))
    const { wrapper } = await mountPage()
    await wrapper.get('input').setValue('分类失败时填写的内容')
    await wrapper.get('form').trigger('submit')
    expect(updateRequest).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toContain('需求分类加载失败')
    await wrapper
      .findAll('button')
      .find((button) => button.text().includes('重新加载分类'))!
      .trigger('click')
    await flushPromises()
    expect(wrapper.get('input').element.value).toBe('分类失败时填写的内容')
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(submitRequest).toHaveBeenCalledTimes(1)
  })

  it('没有可用分类时说明下一步并允许保存草稿', async () => {
    vi.mocked(getEnabledCategories).mockResolvedValue([])
    vi.mocked(createDraft).mockResolvedValue({ id: '102', requestNo: null, status: 'DRAFT' })
    const { wrapper } = await mountPage('/requests/new')
    expect(wrapper.text()).toContain('暂无可用需求分类')
    await wrapper.get('form').trigger('submit')
    expect(createRequest).not.toHaveBeenCalled()
    await wrapper
      .findAll('button')
      .find((button) => button.text().includes('保存草稿'))!
      .trigger('click')
    await flushPromises()
    expect(createDraft).toHaveBeenCalledTimes(1)
  })

  it.each([403, 404])('加载需求返回 %s 时不允许保存或提交', async (status) => {
    vi.mocked(getRequestDetail).mockRejectedValue({ isAxiosError: true, response: { status } })
    const { wrapper, router } = await mountPage()
    expect(wrapper.find('form').exists()).toBe(false)
    expect(router.currentRoute.value.path).toBe('/requests')
    expect(updateRequest).not.toHaveBeenCalled()
    expect(submitRequest).not.toHaveBeenCalled()
  })

  it('不可编辑的需求跳回详情，不展示可提交的表单', async () => {
    vi.mocked(getRequestDetail).mockResolvedValue(detail({ status: 'PENDING_REVIEW' }))
    const { wrapper, router } = await mountPage()
    expect(wrapper.find('form').exists()).toBe(false)
    expect(router.currentRoute.value.path).toBe('/requests/101')
    expect(updateRequest).not.toHaveBeenCalled()
  })

  it('非法编辑编号不能降级成新建需求', async () => {
    const { wrapper, router } = await mountPage('/requests/not-an-id/edit')
    expect(router.currentRoute.value.path).toBe('/requests')
    expect(wrapper.find('form').exists()).toBe(false)
    expect(getRequestDetail).not.toHaveBeenCalled()
    expect(createRequest).not.toHaveBeenCalled()
    expect(createDraft).not.toHaveBeenCalled()
  })

  it('提交阶段发生状态冲突时不自动覆盖或重复提交', async () => {
    vi.mocked(submitRequest).mockRejectedValue({ isAxiosError: true, response: { status: 409 } })
    const { wrapper } = await mountPage()
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('已停止继续保存和提交')
    await wrapper.get('form').trigger('submit')
    expect(updateRequest).toHaveBeenCalledTimes(1)
    expect(submitRequest).toHaveBeenCalledTimes(1)
  })

  it('加载期间不允许写入，离开后的旧请求失败不覆盖当前导航', async () => {
    const pending = deferred<RequestDetail>()
    vi.mocked(getRequestDetail).mockReturnValueOnce(pending.promise)
    const { wrapper, router } = await mountPage()
    await wrapper.get('form').trigger('submit')
    expect(updateRequest).not.toHaveBeenCalled()
    await router.push('/requests/new')
    await flushPromises()
    pending.reject(new Error('late failure'))
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/requests/new')
    expect(wrapper.find('form').exists()).toBe(true)
  })
})

import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CategoryManagementView from './CategoryManagementView.vue'
import CategoryEditorDialog from '@/components/admin/CategoryEditorDialog.vue'
import { createAdminCategory, getAdminCategories } from '@/api/adminCategories'
import type { AdminCategory } from '@/types/admin'

vi.mock('@/api/adminCategories', () => ({
  getAdminCategories: vi.fn(),
  createAdminCategory: vi.fn(),
  updateAdminCategory: vi.fn(),
  changeAdminCategoryStatus: vi.fn(),
}))

const listMock = vi.mocked(getAdminCategories)
const createMock = vi.mocked(createAdminCategory)

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

const category = {
  id: '6',
  name: '网站开发',
  sortOrder: 10,
  enabled: false,
  createdAt: '2026-08-14T08:00:00Z',
  updatedAt: '2026-08-14T08:00:00Z',
} satisfies AdminCategory

function mountView() {
  return shallowMount(CategoryManagementView, {
    global: {
      directives: { loading: () => undefined },
      stubs: {
        'el-alert': true,
        'el-button': ButtonStub,
        'el-card': { template: '<section><slot /></section>' },
        'el-input': true,
        'el-option': true,
        'el-select': true,
        'el-table': true,
        'el-table-column': true,
        'el-tag': true,
      },
    },
  })
}

describe('CategoryManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listMock.mockResolvedValue([category])
  })

  it('加载包含停用项的完整分类列表', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(listMock).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('包含已停用分类')
  })

  it('创建成功后关闭编辑框并重新加载列表', async () => {
    createMock.mockResolvedValue(category)
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('button').trigger('click')

    wrapper.findComponent(CategoryEditorDialog).vm.$emit('submit', {
      name: '网站开发',
      sortOrder: 10,
      reason: '新增业务分类',
    })
    await flushPromises()

    expect(createMock).toHaveBeenCalledWith({
      name: '网站开发',
      sortOrder: 10,
      reason: '新增业务分类',
    })
    expect(listMock).toHaveBeenCalledTimes(2)
  })

  it('DUPLICATE_RESOURCE 409 保留编辑框并显示服务器字段错误', async () => {
    createMock.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 409,
        data: {
          error: {
            code: 'DUPLICATE_RESOURCE',
            message: '分类已存在',
            details: [{ field: 'name', message: '分类名称已被使用' }],
          },
        },
      },
    })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('button').trigger('click')

    wrapper.findComponent(CategoryEditorDialog).vm.$emit('submit', {
      name: '网站开发',
      sortOrder: 10,
      reason: '新增业务分类',
    })
    await flushPromises()

    expect(wrapper.findComponent(CategoryEditorDialog).props('modelValue')).toBe(true)
    expect(wrapper.findComponent(CategoryEditorDialog).props('serverErrors')).toMatchObject({
      name: '分类名称已被使用',
    })
    expect(listMock).toHaveBeenCalledTimes(1)
  })

  it('DATA_VERSION_CONFLICT 409 才关闭编辑框并刷新列表', async () => {
    createMock.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 409,
        data: {
          error: { code: 'DATA_VERSION_CONFLICT', message: '数据已被其他操作更新' },
        },
      },
    })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('button').trigger('click')

    wrapper.findComponent(CategoryEditorDialog).vm.$emit('submit', {
      name: '网站开发',
      sortOrder: 10,
      reason: '新增业务分类',
    })
    await flushPromises()

    expect(wrapper.findComponent(CategoryEditorDialog).props('modelValue')).toBe(false)
    expect(listMock).toHaveBeenCalledTimes(2)
  })
})

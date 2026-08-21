import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessageBox } from 'element-plus'
import MemberManagementView from './MemberManagementView.vue'
import MemberEditorDialog from '@/components/admin/MemberEditorDialog.vue'
import {
  createAdminMember,
  getAdminMember,
  getAdminMembers,
  getAdminSkillTags,
} from '@/api/adminMembers'
import { useAuthStore } from '@/stores/auth'
import type { AdminMember, AdminMemberEditorValue } from '@/types/admin'

vi.mock('@/api/adminMembers', () => ({
  getAdminMembers: vi.fn(),
  getAdminMember: vi.fn(),
  getAdminSkillTags: vi.fn(),
  createAdminMember: vi.fn(),
  updateAdminMember: vi.fn(),
  changeAdminMemberStatus: vi.fn(),
}))

const listMock = vi.mocked(getAdminMembers)
const skillsMock = vi.mocked(getAdminSkillTags)
const createMock = vi.mocked(createAdminMember)
const detailMock = vi.mocked(getAdminMember)

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

const member = {
  id: '12',
  account: 'member01',
  displayName: '成员甲',
  email: null,
  phone: null,
  department: null,
  role: 'MEMBER',
  status: 'ACTIVE',
  skills: [{ id: '2', name: 'Java' }],
  activeOwnerRequestCount: 0,
  createdAt: '2026-08-14T08:00:00Z',
  updatedAt: '2026-08-14T08:00:00Z',
} satisfies AdminMember

const editorValue = {
  account: 'member01',
  initialPassword: 'StrongPassword1!',
  displayName: '成员甲',
  email: '',
  phone: '',
  department: '',
  role: 'MEMBER',
  skillIds: ['2'],
  reason: '新成员加入技术组',
} satisfies AdminMemberEditorValue

function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.user = { id: '1', account: 'admin', displayName: '管理员', role: 'ADMIN' }
  authStore.initialized = true
  return shallowMount(MemberManagementView, {
    global: {
      plugins: [pinia],
      directives: { loading: () => undefined },
      stubs: {
        AppPageHeader: false,
        'el-alert': true,
        'el-button': ButtonStub,
        'el-card': { template: '<section><slot /></section>' },
        'el-form': true,
        'el-form-item': true,
        'el-input': true,
        'el-option': true,
        'el-pagination': true,
        'el-select': true,
        'el-table': true,
        'el-table-column': true,
        'el-tag': true,
        'el-tooltip': true,
      },
    },
  })
}

describe('MemberManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listMock.mockResolvedValue({ items: [member], page: 1, pageSize: 20, total: 1, totalPages: 1 })
    skillsMock.mockResolvedValue([{ id: '2', name: 'Java' }])
    detailMock.mockResolvedValue(member)
  })

  it('进入页面并行加载成员分页和技能标签', async () => {
    mountView()
    await flushPromises()

    expect(listMock).toHaveBeenCalledWith({
      page: 1,
      pageSize: 20,
      keyword: '',
      role: undefined,
      status: undefined,
    })
    expect(skillsMock).toHaveBeenCalledTimes(1)
  })

  it('DUPLICATE_RESOURCE 409 保留编辑框和输入并显示后端字段错误', async () => {
    createMock.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 409,
        data: {
          error: {
            code: 'DUPLICATE_RESOURCE',
            message: '账号已存在',
            details: [{ field: 'account', message: '登录账号已被使用' }],
          },
        },
      },
    })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('button').trigger('click')

    wrapper.findComponent(MemberEditorDialog).vm.$emit('submit', editorValue)
    await flushPromises()

    expect(createMock).toHaveBeenCalledWith(editorValue)
    expect(wrapper.findComponent(MemberEditorDialog).props('serverErrors')).toMatchObject({
      account: '登录账号已被使用',
    })
    expect(wrapper.findComponent(MemberEditorDialog).props('modelValue')).toBe(true)
    expect(listMock).toHaveBeenCalledTimes(1)
  })

  it('新建管理员账号必须经过高权限二次确认', async () => {
    const confirmMock = vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never)
    createMock.mockResolvedValue({ ...member, role: 'ADMIN' })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('button').trigger('click')

    const adminValue = { ...editorValue, role: 'ADMIN' } as const
    wrapper.findComponent(MemberEditorDialog).vm.$emit('submit', adminValue)
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledWith(
      expect.stringContaining('确认创建管理员账号'),
      '确认创建管理员',
      expect.objectContaining({ type: 'warning' }),
    )
    expect(createMock).toHaveBeenCalledWith(adminValue)
    confirmMock.mockRestore()
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

    wrapper.findComponent(MemberEditorDialog).vm.$emit('submit', editorValue)
    await flushPromises()

    expect(wrapper.findComponent(MemberEditorDialog).props('modelValue')).toBe(false)
    expect(listMock).toHaveBeenCalledTimes(2)
  })
})

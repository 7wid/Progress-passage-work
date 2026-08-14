import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  changeAdminMemberStatus,
  createAdminMember,
  getAdminMember,
  getAdminMembers,
  getAdminSkillTags,
  updateAdminMember,
} from './adminMembers'
import { http } from './http'
import type { AdminMember } from '@/types/admin'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)
const putMock = vi.mocked(http.put)

const member = {
  id: '12',
  account: 'member01',
  displayName: '成员甲',
  email: null,
  phone: null,
  department: '计算机学院',
  role: 'MEMBER',
  status: 'ACTIVE',
  skills: [{ id: '2', name: 'Java' }],
  activeOwnerRequestCount: 1,
  createdAt: '2026-08-14T08:00:00Z',
  updatedAt: '2026-08-14T08:00:00Z',
} satisfies AdminMember

describe('admin members api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('按筛选条件分页读取成员并清理关键词', async () => {
    const result = { items: [member], page: 1, pageSize: 20, total: 1, totalPages: 1 }
    getMock.mockResolvedValueOnce({ data: { data: result } } as never)

    await expect(
      getAdminMembers({
        page: 1,
        pageSize: 20,
        keyword: '  成员甲  ',
        role: 'MEMBER',
        status: 'ACTIVE',
      }),
    ).resolves.toEqual(result)

    expect(getMock).toHaveBeenCalledWith('/admin/members', {
      params: {
        page: 1,
        pageSize: 20,
        keyword: '成员甲',
        role: 'MEMBER',
        status: 'ACTIVE',
      },
    })
  })

  it('读取可选技能标签', async () => {
    const skills = [{ id: '2', name: 'Java' }]
    getMock.mockResolvedValueOnce({ data: { data: skills } } as never)

    await expect(getAdminSkillTags()).resolves.toEqual(skills)
    expect(getMock).toHaveBeenCalledWith('/admin/skill-tags')
  })

  it('按十进制正整数编号读取最新成员详情且不丢失长整数字符串精度', async () => {
    getMock.mockResolvedValue({ data: { data: member } } as never)

    await expect(getAdminMember('12')).resolves.toEqual(member)
    expect(getMock).toHaveBeenCalledWith('/admin/members/12')

    await expect(getAdminMember('9007199254740993')).resolves.toEqual(member)
    expect(getMock).toHaveBeenCalledWith('/admin/members/9007199254740993')
  })

  it('获取 CSRF 后创建成员，清理文本但不改写初始密码', async () => {
    getMock.mockResolvedValueOnce({ data: { data: 'csrf-token' } } as never)
    postMock.mockResolvedValueOnce({ data: { data: member } } as never)

    await expect(
      createAdminMember({
        account: '  member01  ',
        initialPassword: '  StrongPassword1!  ',
        displayName: '  成员甲  ',
        email: '  ',
        phone: '  13800138000  ',
        department: '  计算机学院  ',
        role: 'MEMBER',
        skillIds: ['2', '3'],
        reason: '  新成员加入技术组  ',
      }),
    ).resolves.toEqual(member)

    expect(postMock).toHaveBeenCalledWith('/admin/members', {
      account: 'member01',
      initialPassword: '  StrongPassword1!  ',
      displayName: '成员甲',
      email: null,
      phone: '13800138000',
      department: '计算机学院',
      role: 'MEMBER',
      skillIds: [2, 3],
      reason: '新成员加入技术组',
    })
    expect(getMock.mock.invocationCallOrder[0]).toBeLessThan(postMock.mock.invocationCallOrder[0]!)
  })

  it('获取 CSRF 后更新成员或切换状态', async () => {
    getMock.mockResolvedValue({ data: { data: 'csrf-token' } } as never)
    putMock.mockResolvedValueOnce({ data: { data: member } } as never)
    postMock.mockResolvedValueOnce({ data: { data: member } } as never)

    await updateAdminMember('12', {
      expectedUpdatedAt: member.updatedAt,
      displayName: '  成员甲  ',
      email: '',
      phone: '',
      department: '',
      role: 'ADMIN',
      skillIds: ['2'],
      reason: '  调整成员职责范围  ',
    })
    await changeAdminMemberStatus('12', {
      expectedUpdatedAt: member.updatedAt,
      status: 'DISABLED',
      reason: '  成员已经退出技术组  ',
    })

    expect(putMock).toHaveBeenCalledWith('/admin/members/12', {
      expectedUpdatedAt: member.updatedAt,
      displayName: '成员甲',
      email: null,
      phone: null,
      department: null,
      role: 'ADMIN',
      skillIds: [2],
      reason: '调整成员职责范围',
    })
    expect(postMock).toHaveBeenCalledWith('/admin/members/12/status', {
      expectedUpdatedAt: member.updatedAt,
      status: 'DISABLED',
      reason: '成员已经退出技术组',
    })
  })

  it('非法或重复 ID 在获取 CSRF 前被拒绝', async () => {
    await expect(
      updateAdminMember('../12', {
        expectedUpdatedAt: member.updatedAt,
        displayName: '成员甲',
        email: '',
        phone: '',
        department: '',
        role: 'MEMBER',
        skillIds: [],
        reason: '正常调整成员信息',
      }),
    ).rejects.toThrow('成员编号格式不正确')

    await expect(
      createAdminMember({
        account: 'member01',
        initialPassword: 'StrongPassword1!',
        displayName: '成员甲',
        email: '',
        phone: '',
        department: '',
        role: 'MEMBER',
        skillIds: ['2', '2'],
        reason: '新成员加入技术组',
      }),
    ).rejects.toThrow('技能编号不能重复')

    await expect(
      createAdminMember({
        account: 'member01',
        initialPassword: 'StrongPassword1!',
        displayName: '成员甲',
        email: '',
        phone: '',
        department: '',
        role: 'MEMBER',
        skillIds: ['9007199254740993'],
        reason: '新成员加入技术组',
      }),
    ).rejects.toThrow('技能编号格式不正确')

    expect(getMock).not.toHaveBeenCalled()
    expect(postMock).not.toHaveBeenCalled()
    expect(putMock).not.toHaveBeenCalled()
  })
})

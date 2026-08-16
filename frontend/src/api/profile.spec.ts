import { beforeEach, describe, expect, it, vi } from 'vitest'
import { changePassword, getProfile, updateProfile } from './profile'
import { http } from './http'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    patch: vi.fn(),
    put: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const patchMock = vi.mocked(http.patch)
const putMock = vi.mocked(http.put)

describe('profile api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('读取并更新规范化后的个人资料', async () => {
    const profile = {
      id: '1',
      account: 'requester',
      displayName: '需求方',
      email: null,
      phone: null,
      department: null,
      role: 'REQUESTER' as const,
    }
    getMock
      .mockResolvedValueOnce({ data: { data: profile } } as never)
      .mockResolvedValueOnce({ data: { data: 'csrf' } } as never)
    patchMock.mockResolvedValueOnce({ data: { data: profile } } as never)

    await expect(getProfile()).resolves.toEqual(profile)
    await updateProfile({
      displayName: ' 需求方 ',
      email: ' ',
      phone: ' ',
      department: ' 计算机学院 ',
    })

    expect(patchMock).toHaveBeenCalledWith('/users/me', {
      displayName: '需求方',
      email: null,
      phone: null,
      department: '计算机学院',
    })
  })

  it('修改密码前获取 CSRF 令牌', async () => {
    getMock.mockResolvedValueOnce({ data: { data: 'csrf' } } as never)
    putMock.mockResolvedValueOnce({
      data: { data: { otherSessionsInvalidated: true } },
    } as never)

    await expect(
      changePassword({ currentPassword: 'oldPassword1', newPassword: 'newPassword2' }),
    ).resolves.toEqual({ otherSessionsInvalidated: true })
    expect(putMock).toHaveBeenCalledWith('/users/me/password', {
      currentPassword: 'oldPassword1',
      newPassword: 'newPassword2',
    })
  })
})

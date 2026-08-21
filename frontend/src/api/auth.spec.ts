import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getRegistrationStatus, login, register } from './auth'
import { getApiStatus, http } from './http'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
  getApiStatus: vi.fn(),
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)
const getApiStatusMock = vi.mocked(getApiStatus)

describe('registration api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getApiStatusMock.mockReturnValue(undefined)
  })

  it('读取注册开关并提交规范化后的需求方资料', async () => {
    getMock
      .mockResolvedValueOnce({
        data: { data: { enabled: true, emailSuffix: '@example.edu.cn' } },
      } as never)
      .mockResolvedValueOnce({ data: { data: 'csrf' } } as never)
    postMock.mockResolvedValueOnce({
      data: {
        data: {
          id: '10',
          account: 'student01',
          displayName: '学生用户',
          email: 'student@example.edu.cn',
          phone: null,
          department: null,
          role: 'REQUESTER',
        },
      },
    } as never)

    await expect(getRegistrationStatus()).resolves.toEqual({
      enabled: true,
      emailSuffix: '@example.edu.cn',
    })
    await register({
      account: ' student01 ',
      password: 'Password1',
      displayName: ' 学生用户 ',
      email: ' student@example.edu.cn ',
      phone: ' ',
      department: ' ',
    })

    expect(postMock).toHaveBeenCalledWith('/users/register', {
      account: 'student01',
      password: 'Password1',
      displayName: '学生用户',
      email: 'student@example.edu.cn',
      phone: null,
      department: null,
    })
  })
})

describe('login api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getApiStatusMock.mockReturnValue(undefined)
  })

  it('CSRF 校验拒绝时刷新令牌并只重试一次', async () => {
    const csrfError = new Error('csrf rejected')
    getApiStatusMock.mockReturnValue(403)
    getMock.mockResolvedValue({ data: { data: 'csrf' } } as never)
    postMock.mockRejectedValueOnce(csrfError).mockResolvedValueOnce({
      data: {
        data: {
          id: '1',
          account: 'admin',
          displayName: '管理员',
          role: 'ADMIN',
        },
      },
    } as never)

    await expect(login({ account: 'admin', password: 'Password1' })).resolves.toMatchObject({
      account: 'admin',
      role: 'ADMIN',
    })

    expect(getMock).toHaveBeenCalledTimes(2)
    expect(postMock).toHaveBeenCalledTimes(2)
  })
})

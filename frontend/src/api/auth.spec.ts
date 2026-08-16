import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getRegistrationStatus, register } from './auth'
import { http } from './http'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)

describe('registration api', () => {
  beforeEach(() => vi.clearAllMocks())

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

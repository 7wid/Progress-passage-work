import { describe, expect, it } from 'vitest'
import { getLoginErrorMessage } from './http'

function axiosError(status?: number): unknown {
  return {
    isAxiosError: true,
    response: status === undefined ? undefined : { status, data: {} },
  }
}

describe('login error message', () => {
  it('只在 401 时提示账号或密码错误', () => {
    expect(getLoginErrorMessage(axiosError(401))).toBe('账号或密码错误')
  })

  it('接口不存在时提示检查端口和代理', () => {
    expect(getLoginErrorMessage(axiosError(404))).toBe(
      '登录接口不存在，请检查后端端口和前端代理配置',
    )
  })

  it('没有响应时提示后端未启动', () => {
    expect(getLoginErrorMessage(axiosError())).toBe('无法连接后端服务，请确认后端已经启动')
  })
})

import { afterEach, describe, expect, it } from 'vitest'
import { getApiErrorCode, getCookieValue, getLoginErrorMessage, http } from './http'

const originalDocumentCookie = Object.getOwnPropertyDescriptor(document, 'cookie')

afterEach(() => {
  if (originalDocumentCookie) {
    Object.defineProperty(document, 'cookie', originalDocumentCookie)
  } else {
    Reflect.deleteProperty(document, 'cookie')
  }
})

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

  it('登录服务不可用时展示面向用户的提示', () => {
    expect(getLoginErrorMessage(axiosError(404))).toBe('登录服务暂时不可用，请稍后重试')
  })

  it('没有响应时提示系统服务暂时不可用', () => {
    expect(getLoginErrorMessage(axiosError())).toBe('暂时无法连接系统服务，请稍后重试')
  })

  it('服务异常时不向用户暴露内部排查信息', () => {
    expect(getLoginErrorMessage(axiosError(500))).toBe('系统服务暂时不可用，请稍后重试')
  })

  it('403 响应优先展示后端返回的真实原因', () => {
    expect(
      getLoginErrorMessage({
        isAxiosError: true,
        response: {
          status: 403,
          data: { error: { message: '账号已被停用' } },
        },
      }),
    ).toBe('账号已被停用')
  })
})

describe('api error code', () => {
  it('读取后端业务错误码并兼容非 Axios 错误', () => {
    expect(
      getApiErrorCode({
        isAxiosError: true,
        response: { status: 409, data: { error: { code: 'DATA_VERSION_CONFLICT' } } },
      }),
    ).toBe('DATA_VERSION_CONFLICT')
    expect(getApiErrorCode(new Error('network'))).toBeUndefined()
  })
})

describe('csrf request protection', () => {
  it('同名 Cookie 并存时选择最后写入的根路径令牌', () => {
    expect(
      getCookieValue(
        'XSRF-TOKEN=stale-path-token; theme=light; XSRF-TOKEN=fresh%20token',
        'XSRF-TOKEN',
      ),
    ).toBe('fresh token')
  })

  it('为写请求显式注入最新 CSRF Header', async () => {
    Object.defineProperty(document, 'cookie', {
      configurable: true,
      value: 'XSRF-TOKEN=old-token; XSRF-TOKEN=current-token',
    })

    const response = await http.post(
      '/csrf-test',
      {},
      {
        adapter: async (config) => ({
          config,
          data: null,
          headers: {},
          status: 204,
          statusText: 'No Content',
        }),
      },
    )

    expect(response.config.headers.get('X-XSRF-TOKEN')).toBe('current-token')
  })
})

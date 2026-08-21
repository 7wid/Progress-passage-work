import axios from 'axios'
import type { ApiErrorPayload } from '@/types/api'

const XSRF_COOKIE_NAME = 'XSRF-TOKEN'
const XSRF_HEADER_NAME = 'X-XSRF-TOKEN'
const SAFE_HTTP_METHODS = new Set(['get', 'head', 'options', 'trace'])
const PUBLIC_ENTRY_PATHS = new Set(['/', '/login', '/register'])

export function isPublicEntryPath(pathname: string): boolean {
  return PUBLIC_ENTRY_PATHS.has(pathname)
}

export function getCookieValue(cookieHeader: string, name: string): string | undefined {
  let value: string | undefined

  for (const segment of cookieHeader.split(';')) {
    const cookie = segment.trim()
    const separator = cookie.indexOf('=')
    if (separator < 0 || cookie.slice(0, separator).trim() !== name) continue

    const encodedValue = cookie.slice(separator + 1)
    try {
      value = decodeURIComponent(encodedValue)
    } catch {
      value = encodedValue
    }
  }

  return value
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15_000,
  withCredentials: true,
  xsrfCookieName: XSRF_COOKIE_NAME,
  xsrfHeaderName: XSRF_HEADER_NAME,
  withXSRFToken: false,
})

http.interceptors.request.use((config) => {
  const method = config.method?.toLocaleLowerCase() ?? 'get'
  if (SAFE_HTTP_METHODS.has(method) || typeof document === 'undefined') return config

  // 同名 Cookie 按路径长度排序，根路径令牌位于最后；旧的窄路径 Cookie 不应覆盖它。
  const token = getCookieValue(document.cookie, XSRF_COOKIE_NAME)
  if (token) config.headers.set(XSRF_HEADER_NAME, token)
  return config
})

export function getLoginErrorMessage(error: unknown): string {
  if (!axios.isAxiosError<ApiErrorPayload>(error)) {
    return '登录失败，请稍后重试'
  }

  if (!error.response) {
    return '暂时无法连接系统服务，请稍后重试'
  }

  switch (error.response.status) {
    case 400:
      return '账号或密码格式不正确'
    case 401:
      return '账号或密码错误'
    case 403:
      return error.response.data?.error?.message ?? '登录安全校验失败，请刷新页面后重试'
    case 404:
      return '登录服务暂时不可用，请稍后重试'
    default:
      if (error.response.status >= 500) {
        return '系统服务暂时不可用，请稍后重试'
      }
      return error.response.data?.error?.message ?? '登录失败，请稍后重试'
  }
}

export function getApiStatus(error: unknown): number | undefined {
  return axios.isAxiosError<ApiErrorPayload>(error) ? error.response?.status : undefined
}

export function getApiErrorCode(error: unknown): string | undefined {
  return axios.isAxiosError<ApiErrorPayload>(error) ? error.response?.data?.error?.code : undefined
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError<ApiErrorPayload>(error)) {
    return fallback
  }

  if (!error.response) {
    return '暂时无法连接系统服务，请稍后重试'
  }

  return error.response.data?.error?.message ?? fallback
}

export function getApiFieldErrors(error: unknown): Record<string, string> {
  if (!axios.isAxiosError<ApiErrorPayload>(error)) {
    return {}
  }

  const result: Record<string, string> = {}

  for (const detail of error.response?.data?.error?.details ?? []) {
    if (detail.field && result[detail.field] === undefined) {
      result[detail.field] = detail.message
    }
  }

  return result
}

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !isPublicEntryPath(window.location.pathname)) {
      const redirect = encodeURIComponent(window.location.pathname + window.location.search)
      window.location.assign(`/login?redirect=${redirect}`)
    }
    return Promise.reject(error)
  },
)

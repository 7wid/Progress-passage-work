import axios from 'axios'
import type { ApiErrorPayload } from '@/types/api'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15_000,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

export function getLoginErrorMessage(error: unknown): string {
  if (!axios.isAxiosError<ApiErrorPayload>(error)) {
    return '登录失败，请稍后重试'
  }

  if (!error.response) {
    return '无法连接后端服务，请确认后端已经启动'
  }

  switch (error.response.status) {
    case 400:
      return '账号或密码格式不正确'
    case 401:
      return '账号或密码错误'
    case 403:
      return '登录安全校验失败，请刷新页面后重试'
    case 404:
      return '登录接口不存在，请检查后端端口和前端代理配置'
    default:
      if (error.response.status >= 500) {
        return '服务器暂时不可用，请查看后端日志'
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
    return '无法连接后端服务，请确认后端已经启动'
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
    if (error.response?.status === 401 && window.location.pathname !== '/login') {
      const redirect = encodeURIComponent(window.location.pathname + window.location.search)
      window.location.assign(`/login?redirect=${redirect}`)
    }
    return Promise.reject(error)
  },
)

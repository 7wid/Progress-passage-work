export const ACCOUNT_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._-]*[A-Za-z0-9]$/

export function normalizeAccount(account: string): string {
  return account.trim().toLocaleLowerCase()
}

export function getRegistrationPasswordError(password: string): string | null {
  if (password.length < 8 || password.length > 72) {
    return '密码长度应为 8～72 个字符'
  }
  if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
    return '密码必须同时包含字母和数字'
  }
  if (new TextEncoder().encode(password).length > 72) {
    return '密码 UTF-8 编码不能超过 72 字节，请减少中文或特殊字符'
  }
  return null
}

export function resolveSafeRedirect(value: unknown, fallback = '/dashboard'): string {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) {
    return fallback
  }
  return value
}

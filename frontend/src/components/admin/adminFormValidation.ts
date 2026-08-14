export const INITIAL_PASSWORD_VALIDATION_MESSAGE =
  '初始密码需为 8～72 个字符、包含字母和数字，且 UTF-8 编码不超过 72 字节'

export function getInitialPasswordValidationMessage(value: string): string | undefined {
  const byteLength = new TextEncoder().encode(value).length
  const valid =
    value.length >= 8 &&
    value.length <= 72 &&
    byteLength <= 72 &&
    /[A-Za-z]/.test(value) &&
    /\d/.test(value)
  return valid ? undefined : INITIAL_PASSWORD_VALIDATION_MESSAGE
}

export function isValidCategorySortOrder(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0 && value <= 9999
}

import { describe, expect, it } from 'vitest'
import {
  getInitialPasswordValidationMessage,
  INITIAL_PASSWORD_VALIDATION_MESSAGE,
  isValidCategorySortOrder,
} from './adminFormValidation'

describe('管理员表单校验', () => {
  it('初始密码与后端保持相同的字符、组成和 UTF-8 字节限制', () => {
    expect(getInitialPasswordValidationMessage('Abcdefg1')).toBeUndefined()
    expect(getInitialPasswordValidationMessage('abcdefgh')).toBe(
      INITIAL_PASSWORD_VALIDATION_MESSAGE,
    )
    expect(getInitialPasswordValidationMessage('Abc1234')).toBe(INITIAL_PASSWORD_VALIDATION_MESSAGE)
    expect(getInitialPasswordValidationMessage(`${'密'.repeat(24)}A1`)).toBe(
      INITIAL_PASSWORD_VALIDATION_MESSAGE,
    )
  })

  it('分类排序只允许 0～9999 的整数', () => {
    expect(isValidCategorySortOrder(0)).toBe(true)
    expect(isValidCategorySortOrder(9999)).toBe(true)
    expect(isValidCategorySortOrder(1.5)).toBe(false)
    expect(isValidCategorySortOrder(10000)).toBe(false)
  })
})

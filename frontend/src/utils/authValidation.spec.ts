import { describe, expect, it } from 'vitest'
import {
  ACCOUNT_PATTERN,
  getRegistrationPasswordError,
  normalizeAccount,
  resolveSafeRedirect,
} from './authValidation'

describe('authentication validation', () => {
  it('normalizes accounts without changing passwords', () => {
    expect(normalizeAccount(' Student.01 ')).toBe('student.01')
  })

  it('requires stable account delimiters', () => {
    expect(ACCOUNT_PATTERN.test('student-01')).toBe(true)
    expect(ACCOUNT_PATTERN.test('.student')).toBe(false)
    expect(ACCOUNT_PATTERN.test('student_')).toBe(false)
  })

  it('validates password strength and bcrypt byte length', () => {
    expect(getRegistrationPasswordError('Password1')).toBeNull()
    expect(getRegistrationPasswordError('password')).toContain('字母和数字')
    expect(getRegistrationPasswordError(`Password1${'中'.repeat(22)}`)).toContain('72 字节')
  })

  it('only accepts same-site absolute paths as post-login redirects', () => {
    expect(resolveSafeRedirect('/requests/1?tab=progress')).toBe('/requests/1?tab=progress')
    expect(resolveSafeRedirect('https://example.com')).toBe('/dashboard')
    expect(resolveSafeRedirect('//example.com')).toBe('/dashboard')
  })
})

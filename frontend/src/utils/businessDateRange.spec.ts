import { describe, expect, it } from 'vitest'
import { businessDateRange } from './businessDateRange'
describe('业务日期快捷范围', () => {
  it('按上海时间跨月', () => {
    expect(businessDateRange('month', new Date('2026-09-30T16:01:00Z'))).toEqual([
      '2026-10-01',
      '2026-10-01',
    ])
  })
  it('近七天包含今天并正确跨年', () => {
    expect(businessDateRange(7, new Date('2026-01-02T04:00:00Z'))).toEqual([
      '2025-12-27',
      '2026-01-02',
    ])
  })
  it('近三十天包含闰日', () => {
    expect(businessDateRange(30, new Date('2024-03-01T04:00:00Z'))).toEqual([
      '2024-02-01',
      '2024-03-01',
    ])
  })
})

import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type { AdminStatisticsDashboard, StatisticsQuery } from '@/types/statistics'

const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

function requireDate(value: string, label: string): string {
  const match = ISO_DATE_PATTERN.exec(value)
  if (!match) {
    throw new Error(`${label}格式不正确`)
  }
  const year = Number(value.slice(0, 4))
  const month = Number(value.slice(5, 7))
  const day = Number(value.slice(8, 10))
  const date = new Date(Date.UTC(year, month - 1, day))
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    throw new Error(`${label}格式不正确`)
  }
  return value
}

function optionalPositiveId(value: string | undefined): string | undefined {
  if (value === undefined || value === '') return undefined
  if (!/^[1-9]\d*$/.test(value)) throw new Error('分类编号格式不正确')
  return value
}

export async function getAdminStatistics(
  query: StatisticsQuery,
): Promise<AdminStatisticsDashboard> {
  const from = requireDate(query.from, '开始日期')
  const to = requireDate(query.to, '结束日期')
  if (from > to) throw new Error('开始日期不能晚于结束日期')

  const response = await http.get<ApiResponse<AdminStatisticsDashboard>>('/admin/statistics', {
    params: { from, to, categoryId: optionalPositiveId(query.categoryId) },
  })
  return response.data.data
}

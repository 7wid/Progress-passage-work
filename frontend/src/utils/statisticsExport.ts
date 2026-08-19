import type { RequestStatus } from '@/types/request'
import type { AdminStatisticsDashboard, StatisticsRange } from '@/types/statistics'

type CsvCell = string | number

export interface StatisticsExportContext {
  categoryName: string
  statusLabels: Readonly<Record<RequestStatus, string>>
}

const UTF8_BOM = '\uFEFF'
const FORMULA_PREFIX = /^[\t\r ]*[=+\-@]/

function csvCell(value: CsvCell): string {
  const text = String(value)
  const safeText = typeof value === 'string' && FORMULA_PREFIX.test(text) ? `'${text}` : text
  return `"${safeText.replace(/"/g, '""')}"`
}

function csvRow(values: CsvCell[]): string {
  return values.map(csvCell).join(',')
}

function responseCoverage(dashboard: AdminStatisticsDashboard): string {
  const { firstResponseSampleCount, submittedCount } = dashboard.kpis
  if (submittedCount === 0) return '0.00%'
  return `${((firstResponseSampleCount / submittedCount) * 100).toFixed(2)}%`
}

function firstResponseHours(hours: number | null): string | number {
  return hours === null ? '暂无样本' : Number(hours.toFixed(2))
}

export function buildStatisticsCsv(
  dashboard: AdminStatisticsDashboard,
  context: StatisticsExportContext,
): string {
  const { kpis, range } = dashboard
  const rows: CsvCell[][] = [
    ['计算机技术组外包需求管理系统统计报表'],
    ['统计范围', `${range.from} 至 ${range.to}`],
    ['分类筛选', context.categoryName],
    ['数据生成时间', dashboard.generatedAt],
    [],
    ['核心指标', '值'],
    ['新增需求', kpis.submittedCount],
    ['当前已完成', kpis.completedCount],
    ['完成率', `${kpis.completionRate.toFixed(2)}%`],
    ['平均首次响应（小时）', firstResponseHours(kpis.averageFirstResponseHours)],
    ['首次响应样本数', kpis.firstResponseSampleCount],
    ['响应样本覆盖率', responseCoverage(dashboard)],
    [],
    ['状态分布'],
    ['状态', '数量'],
    ...(dashboard.statusDistribution.length
      ? dashboard.statusDistribution.map((item) => [context.statusLabels[item.status], item.count])
      : [['暂无数据', '']]),
    [],
    ['分类分布'],
    ['分类', '数量'],
    ...(dashboard.categoryDistribution.length
      ? dashboard.categoryDistribution.map((item) => [item.categoryName, item.count])
      : [['暂无数据', '']]),
    [],
    ['新增趋势'],
    ['日期', '新增需求'],
    ...(dashboard.submissionTrend.length
      ? dashboard.submissionTrend.map((item) => [item.date, item.count])
      : [['暂无数据', '']]),
    [],
    ['成员负载'],
    ['成员', '账号', '当前负载', '处理中', '待验收'],
    ...(dashboard.memberWorkloads.length
      ? dashboard.memberWorkloads.map((item) => [
          item.memberName,
          item.memberAccount,
          item.activeCount,
          item.inProgressCount,
          item.pendingAcceptanceCount,
        ])
      : [['暂无数据', '', '', '', '']]),
  ]

  return UTF8_BOM + rows.map(csvRow).join('\r\n')
}

export function statisticsCsvFilename(range: StatisticsRange): string {
  return `需求统计_${range.from}_${range.to}.csv`
}

export function downloadStatisticsCsv(
  dashboard: AdminStatisticsDashboard,
  context: StatisticsExportContext,
): string {
  const filename = statisticsCsvFilename(dashboard.range)
  const blob = new Blob([buildStatisticsCsv(dashboard, context)], {
    type: 'text/csv;charset=utf-8',
  })
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  link.hidden = true
  document.body.appendChild(link)

  try {
    link.click()
  } finally {
    link.remove()
    URL.revokeObjectURL(objectUrl)
  }

  return filename
}

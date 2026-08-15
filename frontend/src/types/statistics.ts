import type { RequestStatus } from '@/types/request'

export interface StatisticsQuery {
  from: string
  to: string
  categoryId?: string
}

export interface StatisticsRange {
  from: string
  to: string
  categoryId: string | null
}

export interface StatisticsKpis {
  submittedCount: number
  completedCount: number
  completionRate: number
  firstResponseSampleCount: number
  averageFirstResponseHours: number | null
}

export interface StatusCount {
  status: RequestStatus
  count: number
}

export interface CategoryCount {
  categoryId: string
  categoryName: string
  count: number
}

export interface DailyRequestCount {
  date: string
  count: number
}

export interface AdminStatisticsDashboard {
  range: StatisticsRange
  kpis: StatisticsKpis
  statusDistribution: StatusCount[]
  categoryDistribution: CategoryCount[]
  submissionTrend: DailyRequestCount[]
  generatedAt: string
}

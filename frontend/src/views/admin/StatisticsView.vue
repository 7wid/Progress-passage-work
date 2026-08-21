<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  BadgeCheck,
  ChartPie,
  ChartNoAxesCombined,
  Download,
  FilePlus2,
  Gauge,
  RotateCcw,
  Search,
  Shapes,
  Timer,
  TrendingUp,
  UsersRound,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'
import { getAdminCategories } from '@/api/adminCategories'
import { getApiErrorMessage } from '@/api/http'
import { getAdminStatistics } from '@/api/statistics'
import MemberWorkloadTable from '@/components/statistics/MemberWorkloadTable.vue'
import StatisticsTrendChart from '@/components/statistics/StatisticsTrendChart.vue'
import AppPageHeader from '@/components/common/AppPageHeader.vue'
import AppSectionHeader from '@/components/common/AppSectionHeader.vue'
import type { AdminCategory } from '@/types/admin'
import type { RequestStatus } from '@/types/request'
import type { AdminStatisticsDashboard } from '@/types/statistics'
import { downloadStatisticsCsv } from '@/utils/statisticsExport'

const BUSINESS_TIME_ZONE = 'Asia/Shanghai'
const statusLabels: Record<RequestStatus, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待评估',
  NEED_MORE_INFO: '待补充',
  PENDING_ASSIGNMENT: '待分配',
  IN_PROGRESS: '处理中',
  PENDING_ACCEPTANCE: '待验收',
  COMPLETED: '已完成',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
}

function businessDateParts(): { year: string; month: string; day: string } {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: BUSINESS_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const value = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? ''
  return { year: value('year'), month: value('month'), day: value('day') }
}

function currentMonthRange(): [string, string] {
  const { year, month, day } = businessDateParts()
  return [`${year}-${month}-01`, `${year}-${month}-${day}`]
}

const loading = ref(false)
const categoryLoading = ref(false)
const errorMessage = ref('')
const categoryError = ref('')
const dashboard = ref<AdminStatisticsDashboard | null>(null)
const categories = ref<AdminCategory[]>([])
const dateRange = ref<[string, string]>(currentMonthRange())
const categoryId = ref('')
let loadSequence = 0

const integerFormatter = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 })
const generatedAtFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})
const maxStatusCount = computed(() =>
  Math.max(1, ...(dashboard.value?.statusDistribution.map((item) => item.count) ?? [])),
)
const maxCategoryCount = computed(() =>
  Math.max(1, ...(dashboard.value?.categoryDistribution.map((item) => item.count) ?? [])),
)
const responseCoverage = computed(() => {
  const kpis = dashboard.value?.kpis
  if (!kpis || kpis.submittedCount === 0) return '0%'
  return `${Math.round((kpis.firstResponseSampleCount / kpis.submittedCount) * 100)}%`
})
const exportCategoryName = computed(() => {
  const exportedCategoryId = dashboard.value?.range.categoryId
  if (!exportedCategoryId) return '全部分类'
  return (
    categories.value.find((category) => category.id === exportedCategoryId)?.name ??
    `分类编号 ${exportedCategoryId}`
  )
})

function barWidth(count: number, maximum: number): string {
  return count === 0 ? '0%' : `${Math.max(3, (count / maximum) * 100)}%`
}

function formatFirstResponse(hours: number | null): string {
  if (hours === null) return '暂无样本'
  if (hours < 1 / 60) return '不足 1 分钟'
  if (hours < 1) return `${Math.round(hours * 60)} 分钟`
  return `${hours.toFixed(2)} 小时`
}

function formatGeneratedAt(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : generatedAtFormatter.format(date)
}

async function loadCategories(): Promise<void> {
  categoryLoading.value = true
  categoryError.value = ''
  try {
    categories.value = await getAdminCategories()
  } catch (error) {
    categories.value = []
    categoryError.value = getApiErrorMessage(error, '分类筛选项加载失败')
  } finally {
    categoryLoading.value = false
  }
}

async function loadDashboard(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getAdminStatistics({
      from: dateRange.value[0],
      to: dateRange.value[1],
      categoryId: categoryId.value || undefined,
    })
    if (sequence === loadSequence) dashboard.value = result
  } catch (error) {
    if (sequence !== loadSequence) return
    dashboard.value = null
    errorMessage.value = getApiErrorMessage(error, '统计数据加载失败，请稍后重试')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function resetFilters(): void {
  dateRange.value = currentMonthRange()
  categoryId.value = ''
  void loadDashboard()
}

function exportDashboard(): void {
  if (!dashboard.value || loading.value) return
  try {
    const filename = downloadStatisticsCsv(dashboard.value, {
      categoryName: exportCategoryName.value,
      statusLabels,
    })
    ElMessage.success(`已导出 ${filename}`)
  } catch {
    ElMessage.error('报表导出失败，请重试')
  }
}

onMounted(() => {
  void loadCategories()
  void loadDashboard()
})
</script>

<template>
  <section class="page">
    <AppPageHeader
      title="数据概览"
      description="按发起日期统计需求处理情况与首次评估响应效率。"
      eyebrow="ANALYTICS"
      :icon="ChartNoAxesCombined"
      tone="purple"
    >
      <template #meta>
        <span v-if="dashboard" class="generated-at">
          数据生成于 {{ formatGeneratedAt(dashboard.generatedAt) }}
        </span>
      </template>
      <template #actions>
        <el-button :disabled="!dashboard || loading" @click="exportDashboard">
          <Download :size="16" aria-hidden="true" />
          导出 CSV
        </el-button>
      </template>
    </AppPageHeader>

    <el-card>
      <div class="filters">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
        />
        <el-select
          v-model="categoryId"
          clearable
          filterable
          :loading="categoryLoading"
          placeholder="全部分类"
        >
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.enabled ? category.name : `${category.name}（已停用）`"
            :value="category.id"
          />
        </el-select>
        <div class="filter-actions">
          <el-button @click="resetFilters">
            <RotateCcw :size="16" aria-hidden="true" />
            重置为本月
          </el-button>
          <el-button type="primary" :loading="loading" @click="loadDashboard">
            <Search :size="16" aria-hidden="true" />
            查询
          </el-button>
        </div>
      </div>
      <p v-if="categoryError" class="filter-warning">{{ categoryError }}</p>
    </el-card>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-alert>

    <template v-if="dashboard">
      <div class="kpi-grid" v-loading="loading">
        <el-card shadow="never" class="kpi-card kpi-card--blue">
          <div class="kpi-card__heading">
            <span class="kpi-card__icon" aria-hidden="true"><FilePlus2 :size="19" /></span>
            <span>新增需求</span>
          </div>
          <strong>{{ integerFormatter.format(dashboard.kpis.submittedCount) }}</strong>
          <small>所选范围内已发起，不包含草稿</small>
        </el-card>
        <el-card shadow="never" class="kpi-card kpi-card--green">
          <div class="kpi-card__heading">
            <span class="kpi-card__icon" aria-hidden="true"><BadgeCheck :size="19" /></span>
            <span>当前已完成</span>
          </div>
          <strong>{{ integerFormatter.format(dashboard.kpis.completedCount) }}</strong>
          <small>完成率 {{ dashboard.kpis.completionRate.toFixed(2) }}%</small>
        </el-card>
        <el-card shadow="never" class="kpi-card kpi-card--orange">
          <div class="kpi-card__heading">
            <span class="kpi-card__icon" aria-hidden="true"><Timer :size="19" /></span>
            <span>平均首次响应</span>
          </div>
          <strong>{{ formatFirstResponse(dashboard.kpis.averageFirstResponseHours) }}</strong>
          <small>第一条评估距发起时间</small>
        </el-card>
        <el-card shadow="never" class="kpi-card kpi-card--purple">
          <div class="kpi-card__heading">
            <span class="kpi-card__icon" aria-hidden="true"><Gauge :size="19" /></span>
            <span>响应样本覆盖</span>
          </div>
          <strong>{{ responseCoverage }}</strong>
          <small>
            {{ dashboard.kpis.firstResponseSampleCount }} / {{ dashboard.kpis.submittedCount }} 条
          </small>
        </el-card>
      </div>

      <el-alert
        type="info"
        :closable="false"
        title="统计口径：按需求发起日期纳入样本；完成数取当前状态；首次响应取第一条可行性评估。"
      />

      <div class="dashboard-grid">
        <el-card class="trend-card">
          <template #header>
            <AppSectionHeader
              title="新增趋势"
              description="所选日期范围内的需求发起量"
              :icon="TrendingUp"
            />
          </template>
          <StatisticsTrendChart :data="dashboard.submissionTrend" />
        </el-card>

        <el-card>
          <template #header>
            <AppSectionHeader
              title="状态分布"
              description="当前处理状态构成"
              :icon="ChartPie"
              tone="green"
            />
          </template>
          <div class="distribution-list">
            <div v-for="item in dashboard.statusDistribution" :key="item.status" class="bar-row">
              <div class="bar-meta">
                <span>{{ statusLabels[item.status] }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <div class="bar-track">
                <span :style="{ width: barWidth(item.count, maxStatusCount) }" />
              </div>
            </div>
          </div>
        </el-card>

        <el-card>
          <template #header>
            <AppSectionHeader
              title="分类分布"
              description="需求分类数量对比"
              :icon="Shapes"
              tone="orange"
            />
          </template>
          <div v-if="dashboard.categoryDistribution.length" class="distribution-list">
            <div
              v-for="item in dashboard.categoryDistribution"
              :key="item.categoryId"
              class="bar-row"
            >
              <div class="bar-meta">
                <span>{{ item.categoryName }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <div class="bar-track bar-track--category">
                <span :style="{ width: barWidth(item.count, maxCategoryCount) }" />
              </div>
            </div>
          </div>
          <el-empty v-else description="所选范围内暂无分类数据" />
        </el-card>

        <el-card class="member-workload-card">
          <template #header>
            <AppSectionHeader
              title="成员负载"
              description="按主负责人统计所选范围内当前未终结需求"
              :icon="UsersRound"
              tone="purple"
            />
          </template>
          <MemberWorkloadTable :data="dashboard.memberWorkloads" :loading="loading" />
        </el-card>
      </div>
    </template>

    <el-skeleton v-else-if="loading" :rows="8" animated />
  </section>
</template>

<style scoped>
.summary,
.generated-at,
.kpi-card small {
  color: var(--color-text-tertiary);
}
.generated-at {
  padding: 5px 10px;
  background: var(--color-surface-secondary);
  border: 1px solid var(--color-border-subtle);
  border-radius: 999px;
  font-size: 13px;
}
.filters {
  display: grid;
  grid-template-columns: minmax(300px, 1.4fr) minmax(200px, 1fr) auto;
  gap: 12px;
}
.filters :deep(.el-date-editor),
.filters :deep(.el-select) {
  width: 100%;
}
.filter-actions {
  display: flex;
  gap: 8px;
}
.filter-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}
.filter-warning {
  margin: 10px 0 0;
  color: var(--color-warning);
  font-size: 13px;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.kpi-card :deep(.el-card__body) {
  display: grid;
  gap: 9px;
  min-height: 160px;
  align-content: start;
}
.kpi-card {
  --kpi-color: var(--color-primary);
  --kpi-soft: var(--color-primary-soft);
  border-top: 2px solid var(--kpi-color);
  animation: kpi-enter 360ms var(--ease-standard) both;
}
.kpi-card:nth-child(2) {
  animation-delay: 45ms;
}
.kpi-card:nth-child(3) {
  animation-delay: 90ms;
}
.kpi-card:nth-child(4) {
  animation-delay: 135ms;
}
.kpi-card--green {
  --kpi-color: var(--color-success);
  --kpi-soft: #ecfdf5;
}
.kpi-card--orange {
  --kpi-color: var(--color-warning);
  --kpi-soft: #fff7ed;
}
.kpi-card--purple {
  --kpi-color: var(--color-purple);
  --kpi-soft: #f5f3ff;
}
.kpi-card__heading {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 600;
}
.kpi-card__icon {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  color: var(--kpi-color);
  background: var(--kpi-soft);
  border-radius: var(--radius-md);
}
.kpi-card strong {
  color: var(--color-text-primary);
  font-size: 28px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.trend-card {
  grid-column: 1 / -1;
}
.member-workload-card {
  grid-column: 1 / -1;
}
.distribution-list {
  display: grid;
  gap: 15px;
}
.bar-row {
  display: grid;
  gap: 7px;
}
.bar-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.bar-track {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--color-surface-secondary);
}
.bar-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary);
  transition: width 0.2s ease;
}
.bar-track--category span {
  background: var(--color-purple);
}

@keyframes kpi-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .filters,
  .kpi-grid,
  .dashboard-grid {
    grid-template-columns: 1fr 1fr;
  }
  .filter-actions,
  .trend-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 640px) {
  .filters,
  .kpi-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
  .filter-actions {
    grid-column: auto;
  }
  .generated-at {
    align-self: flex-start;
  }
}
</style>

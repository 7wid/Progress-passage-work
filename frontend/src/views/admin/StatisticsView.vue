<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getAdminCategories } from '@/api/adminCategories'
import { getApiErrorMessage } from '@/api/http'
import { getAdminStatistics } from '@/api/statistics'
import StatisticsTrendChart from '@/components/statistics/StatisticsTrendChart.vue'
import type { AdminCategory } from '@/types/admin'
import type { RequestStatus } from '@/types/request'
import type { AdminStatisticsDashboard } from '@/types/statistics'

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

onMounted(() => {
  void loadCategories()
  void loadDashboard()
})
</script>

<template>
  <section class="page">
    <div class="page__header">
      <div>
        <h2>数据概览</h2>
        <span class="summary">按提交日期统计需求处理情况与首次评估响应效率</span>
      </div>
      <span v-if="dashboard" class="generated-at">
        数据生成于 {{ formatGeneratedAt(dashboard.generatedAt) }}
      </span>
    </div>

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
          <el-button @click="resetFilters">重置为本月</el-button>
          <el-button type="primary" :loading="loading" @click="loadDashboard">查询</el-button>
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
        <el-card shadow="never" class="kpi-card">
          <span>新增需求</span>
          <strong>{{ integerFormatter.format(dashboard.kpis.submittedCount) }}</strong>
          <small>所选范围内已提交，不包含草稿</small>
        </el-card>
        <el-card shadow="never" class="kpi-card">
          <span>当前已完成</span>
          <strong>{{ integerFormatter.format(dashboard.kpis.completedCount) }}</strong>
          <small>完成率 {{ dashboard.kpis.completionRate.toFixed(2) }}%</small>
        </el-card>
        <el-card shadow="never" class="kpi-card">
          <span>平均首次响应</span>
          <strong>{{ formatFirstResponse(dashboard.kpis.averageFirstResponseHours) }}</strong>
          <small>第一条评估距提交时间</small>
        </el-card>
        <el-card shadow="never" class="kpi-card">
          <span>响应样本覆盖</span>
          <strong>{{ responseCoverage }}</strong>
          <small>
            {{ dashboard.kpis.firstResponseSampleCount }} / {{ dashboard.kpis.submittedCount }} 条
          </small>
        </el-card>
      </div>

      <el-alert
        type="info"
        :closable="false"
        title="统计口径：按需求提交日期纳入样本；完成数取当前状态；首次响应取第一条可行性评估。"
      />

      <div class="dashboard-grid">
        <el-card class="trend-card">
          <template #header><strong>新增趋势</strong></template>
          <StatisticsTrendChart :data="dashboard.submissionTrend" />
        </el-card>

        <el-card>
          <template #header><strong>状态分布</strong></template>
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
          <template #header><strong>分类分布</strong></template>
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
      </div>
    </template>

    <el-skeleton v-else-if="loading" :rows="8" animated />
  </section>
</template>

<style scoped>
.summary,
.generated-at,
.kpi-card small {
  color: #6b7280;
}
.generated-at {
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
.filter-warning {
  margin: 10px 0 0;
  color: #b45309;
  font-size: 13px;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.kpi-card :deep(.el-card__body) {
  display: grid;
  gap: 8px;
}
.kpi-card strong {
  color: #111827;
  font-size: 28px;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.trend-card {
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
  background: #edf2f7;
}
.bar-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #409eff;
  transition: width 0.2s ease;
}
.bar-track--category span {
  background: #67c23a;
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
}
</style>

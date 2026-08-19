<script setup lang="ts">
import { computed, markRaw, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import {
  Activity,
  ArrowRight,
  CheckCircle2,
  CircleDot,
  ClipboardClock,
  Clock3,
  ListFilter,
  Plus,
  RefreshCw,
} from '@lucide/vue'
import { useRouter } from 'vue-router'
import { getRequests } from '@/api/requests'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type { RequestStatus, RequestSummary } from '@/types/request'

interface MetricConfig {
  label: string
  status: RequestStatus
  tone: string
  hint: string
  icon: Component
}

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const total = ref(0)
const recent = ref<RequestSummary[]>([])
const counts = ref<Partial<Record<RequestStatus, number>>>({})

const requesterCards: MetricConfig[] = [
  {
    label: '待评估',
    status: 'PENDING_REVIEW',
    tone: 'blue',
    hint: '等待技术组响应',
    icon: markRaw(ClipboardClock),
  },
  {
    label: '处理中',
    status: 'IN_PROGRESS',
    tone: 'green',
    hint: '正在协作推进',
    icon: markRaw(CircleDot),
  },
  {
    label: '待验收',
    status: 'PENDING_ACCEPTANCE',
    tone: 'orange',
    hint: '等待确认成果',
    icon: markRaw(Clock3),
  },
  {
    label: '已完成',
    status: 'COMPLETED',
    tone: 'purple',
    hint: '已闭环交付',
    icon: markRaw(CheckCircle2),
  },
]

const teamCards: MetricConfig[] = [
  {
    label: '待评估',
    status: 'PENDING_REVIEW',
    tone: 'blue',
    hint: '需要给出可行性结论',
    icon: markRaw(ClipboardClock),
  },
  {
    label: '待分配',
    status: 'PENDING_ASSIGNMENT',
    tone: 'orange',
    hint: '等待明确负责人',
    icon: markRaw(Clock3),
  },
  {
    label: '处理中',
    status: 'IN_PROGRESS',
    tone: 'green',
    hint: '当前执行中的任务',
    icon: markRaw(CircleDot),
  },
  {
    label: '待验收',
    status: 'PENDING_ACCEPTANCE',
    tone: 'purple',
    hint: '成果已提交待确认',
    icon: markRaw(CheckCircle2),
  },
]

const cards = computed(() => (authStore.user?.role === 'REQUESTER' ? requesterCards : teamCards))
const heading = computed(() =>
  authStore.user?.role === 'REQUESTER' ? '我的需求概览' : '需求处理概览',
)
const canCreate = computed(
  () => authStore.user?.role === 'REQUESTER' || authStore.user?.role === 'ADMIN',
)
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [latest, ...statusResults] = await Promise.all([
      getRequests({ page: 1, pageSize: 5, sort: 'NEWEST' }),
      ...cards.value.map((card) =>
        getRequests({ page: 1, pageSize: 1, status: card.status, sort: 'NEWEST' }),
      ),
    ])
    total.value = latest.total
    recent.value = latest.items
    counts.value = Object.fromEntries(
      cards.value.map((card, index) => [card.status, statusResults[index]?.total ?? 0]),
    )
  } catch {
    total.value = 0
    recent.value = []
    counts.value = {}
    errorMessage.value = '首页数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function openStatus(status: RequestStatus) {
  void router.push({ name: 'request-list', query: { status } })
}

function openRequest(id: string) {
  void router.push({ name: 'request-detail', params: { id } })
}

onMounted(loadDashboard)
</script>

<template>
  <section class="page dashboard-page">
    <div class="dashboard-intro">
      <div class="dashboard-intro__copy">
        <span class="dashboard-intro__date">
          <Activity :size="14" aria-hidden="true" />
          {{ today }}
        </span>
        <h1>{{ greeting }}，{{ authStore.user?.displayName }}</h1>
        <p>集中查看需求状态、近期更新与需要继续推进的工作。</p>
      </div>
      <div class="dashboard-intro__actions">
        <el-button :loading="loading" @click="loadDashboard">
          <RefreshCw :size="17" aria-hidden="true" />
          刷新数据
        </el-button>
        <el-button v-if="canCreate" type="primary" @click="router.push('/requests/new')">
          <Plus :size="17" aria-hidden="true" />
          提交需求
        </el-button>
      </div>
    </div>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage">
      <template #default>
        <el-button link type="primary" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-alert>

    <div class="overview-heading">
      <div>
        <h2>{{ heading }}</h2>
        <span class="total">共 {{ total }} 条需求</span>
      </div>
      <button type="button" class="text-action" @click="router.push('/requests')">
        查看全部
        <ArrowRight :size="16" aria-hidden="true" />
      </button>
    </div>

    <div v-loading="loading" class="metrics" aria-label="需求状态概览">
      <button
        v-for="card in cards"
        :key="card.status"
        type="button"
        class="metric"
        :class="`metric--${card.tone}`"
        @click="openStatus(card.status)"
      >
        <span class="metric__icon" aria-hidden="true">
          <component :is="card.icon" :size="19" :stroke-width="1.8" />
        </span>
        <span class="metric__content">
          <span>{{ card.label }}</span>
          <small>{{ card.hint }}</small>
        </span>
        <strong>{{ counts[card.status] ?? 0 }}</strong>
      </button>
    </div>

    <el-card class="recent-card">
      <template #header>
        <div class="card-heading">
          <div>
            <strong>最近需求</strong>
            <span>按最近更新时间排列</span>
          </div>
          <ListFilter :size="18" aria-hidden="true" />
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="recent"
        row-key="id"
        empty-text="暂无需求"
        class="recent-table"
        @row-click="(row: RequestSummary) => openRequest(row.id)"
      >
        <el-table-column label="需求编号" width="170">
          <template #default="{ row }">{{ row.requestNo ?? '草稿' }}</template>
        </el-table-column>
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <button type="button" class="request-title-link" @click.stop="openRequest(row.id)">
              {{ row.title }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><RequestStatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="期望日期" width="120">
          <template #default="{ row }">{{ row.expectedDeadline ?? '—' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
.dashboard-intro {
  position: relative;
  display: flex;
  min-height: 190px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding: 34px 38px;
  overflow: hidden;
  color: #f7fffb;
  background: #1d4ed8;
  border: 1px solid #1d4ed8;
  border-radius: var(--radius-lg);
  animation: intro-enter 380ms var(--ease-standard) both;
}

.dashboard-intro::before,
.dashboard-intro::after {
  position: absolute;
  content: '';
  pointer-events: none;
}

.dashboard-intro::before {
  top: -110px;
  right: 9%;
  width: 250px;
  height: 250px;
  border: 1px solid rgb(255 255 255 / 12%);
  transform: rotate(24deg);
}

.dashboard-intro::after {
  right: -45px;
  bottom: -90px;
  width: 160px;
  height: 260px;
  background: rgb(96 165 250 / 20%);
  transform: rotate(28deg);
}

.dashboard-intro > * {
  position: relative;
  z-index: 1;
}

.dashboard-intro__copy {
  max-width: 660px;
}

.dashboard-intro__date {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 9px;
  color: #dbeafe;
  font-size: 13px;
  font-weight: 600;
}

.dashboard-intro h1 {
  margin: 0;
  font-size: 42px;
  font-weight: 680;
  line-height: 1.2;
}

.dashboard-intro p {
  margin: 12px 0 0;
  color: #dbeafe;
  font-size: 15px;
}

.dashboard-intro__actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}

.dashboard-intro__actions :deep(.el-button) {
  min-height: 42px;
  margin: 0;
}

.dashboard-intro__actions :deep(.el-button:not(.el-button--primary)) {
  color: #f7fffb;
  background: rgb(255 255 255 / 8%);
  border-color: rgb(255 255 255 / 28%);
}

.dashboard-intro__actions :deep(.el-button--primary) {
  color: #1d4ed8;
  background: #ffffff;
  border-color: #ffffff;
}

.dashboard-intro__actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.overview-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: -8px;
}

.overview-heading h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 19px;
  font-weight: 650;
}

.total {
  display: block;
  margin-top: 3px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.text-action {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  color: var(--color-primary-strong);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
}

.text-action:hover {
  background: var(--color-primary-soft);
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  min-height: 102px;
}

.metric {
  display: grid;
  min-width: 0;
  min-height: 102px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 17px;
  text-align: left;
  color: var(--color-text-primary);
  background: var(--color-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition:
    border-color var(--motion-fast) ease,
    box-shadow var(--motion-fast) ease,
    background-color var(--motion-fast) ease;
  animation: metric-enter 360ms var(--ease-standard) both;
}

.metric:nth-child(2) {
  animation-delay: 45ms;
}

.metric:nth-child(3) {
  animation-delay: 90ms;
}

.metric:nth-child(4) {
  animation-delay: 135ms;
}

.metric:hover {
  background: #fcfdfc;
  border-color: var(--color-border);
  box-shadow: 0 5px 16px rgb(20 32 27 / 6%);
}

.metric__icon {
  display: inline-grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: var(--metric-color, var(--color-text-secondary));
  background: var(--metric-background, var(--color-surface-secondary));
  border-radius: var(--radius-md);
}

.metric__content {
  display: grid;
  min-width: 0;
  gap: 2px;
  font-size: 14px;
  font-weight: 620;
}

.metric__content small {
  overflow: hidden;
  color: var(--color-text-tertiary);
  font-size: 11px;
  font-weight: 450;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric strong {
  color: var(--color-text-primary);
  font-size: 27px;
  font-weight: 680;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.metric--blue {
  --metric-color: #2563eb;
  --metric-background: #eff6ff;
}

.metric--green {
  --metric-color: #059669;
  --metric-background: #ecfdf5;
}

.metric--orange {
  --metric-color: #d97706;
  --metric-background: #fffbeb;
}

.metric--purple {
  --metric-color: #7c3aed;
  --metric-background: #f5f3ff;
}

@keyframes intro-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
}

@keyframes metric-enter {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
}

.recent-card {
  min-width: 0;
}

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--color-text-tertiary);
}

.card-heading > div {
  display: grid;
  gap: 2px;
}

.card-heading strong {
  color: var(--color-text-primary);
  font-size: 15px;
}

.card-heading span {
  font-size: 12px;
  font-weight: 450;
}

.recent-card :deep(.el-card__body) {
  padding: 0;
}

.recent-table :deep(.el-table__row) {
  cursor: pointer;
}

.request-title-link {
  display: block;
  max-width: 100%;
  padding: 4px 0;
  overflow: hidden;
  color: var(--color-text-primary);
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
  border: 0;
  font: inherit;
  font-weight: 580;
}

.request-title-link:hover {
  color: var(--color-primary-strong);
}

@media (max-width: 1180px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 840px) {
  .dashboard-intro {
    min-height: 0;
    align-items: flex-start;
    flex-direction: column;
    padding: 28px;
  }
}

@media (max-width: 560px) {
  .dashboard-intro {
    padding: 24px 20px;
  }

  .dashboard-intro h1 {
    font-size: 32px;
  }

  .dashboard-intro__actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .dashboard-intro__actions :deep(.el-button) {
    flex: 1;
  }

  .metrics {
    grid-template-columns: 1fr;
  }
}
</style>

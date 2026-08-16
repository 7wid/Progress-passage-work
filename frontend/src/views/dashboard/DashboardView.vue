<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRequests } from '@/api/requests'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type { RequestStatus, RequestSummary } from '@/types/request'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const total = ref(0)
const recent = ref<RequestSummary[]>([])
const counts = ref<Partial<Record<RequestStatus, number>>>({})

const requesterCards: Array<{ label: string; status: RequestStatus; tone: string }> = [
  { label: '待评估', status: 'PENDING_REVIEW', tone: 'blue' },
  { label: '处理中', status: 'IN_PROGRESS', tone: 'green' },
  { label: '待验收', status: 'PENDING_ACCEPTANCE', tone: 'orange' },
  { label: '已完成', status: 'COMPLETED', tone: 'gray' },
]

const teamCards: Array<{ label: string; status: RequestStatus; tone: string }> = [
  { label: '待评估', status: 'PENDING_REVIEW', tone: 'blue' },
  { label: '待分配', status: 'PENDING_ASSIGNMENT', tone: 'orange' },
  { label: '处理中', status: 'IN_PROGRESS', tone: 'green' },
  { label: '待验收', status: 'PENDING_ACCEPTANCE', tone: 'gray' },
]

const cards = computed(() => (authStore.user?.role === 'REQUESTER' ? requesterCards : teamCards))
const heading = computed(() =>
  authStore.user?.role === 'REQUESTER' ? '我的需求概览' : '需求处理概览',
)

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
  if (authStore.user?.role !== 'REQUESTER') {
    void router.push({ name: 'request-list', query: { status } })
    return
  }
  void router.push({ name: 'request-list', query: { status } })
}

function openRequest(id: string) {
  void router.push({ name: 'request-detail', params: { id } })
}

onMounted(loadDashboard)
</script>

<template>
  <section class="page">
    <div class="page__header">
      <div>
        <h2>{{ heading }}</h2>
        <span class="total">共 {{ total }} 条需求</span>
      </div>
      <el-button :loading="loading" @click="loadDashboard">刷新</el-button>
    </div>

    <el-alert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" />

    <div v-loading="loading" class="metrics">
      <button
        v-for="card in cards"
        :key="card.status"
        type="button"
        class="metric"
        :class="`metric--${card.tone}`"
        @click="openStatus(card.status)"
      >
        <span>{{ card.label }}</span>
        <strong>{{ counts[card.status] ?? 0 }}</strong>
      </button>
    </div>

    <el-card header="最近需求">
      <el-table
        v-loading="loading"
        :data="recent"
        row-key="id"
        empty-text="暂无需求"
        @row-click="(row: RequestSummary) => openRequest(row.id)"
      >
        <el-table-column label="需求编号" width="180">
          <template #default="{ row }">{{ row.requestNo ?? '草稿' }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
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
.total {
  color: #6b7280;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  min-height: 104px;
}

.metric {
  display: grid;
  min-width: 0;
  padding: 18px;
  text-align: left;
  color: #374151;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-top: 4px solid #909399;
  border-radius: 6px;
  cursor: pointer;
}

.metric strong {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
}

.metric--blue {
  border-top-color: #409eff;
}
.metric--green {
  border-top-color: #67c23a;
}
.metric--orange {
  border-top-color: #e6a23c;
}
.metric--gray {
  border-top-color: #909399;
}

@media (max-width: 900px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .metrics {
    grid-template-columns: 1fr;
  }
}
</style>

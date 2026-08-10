<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { confirmEvaluationRejection, getEvaluations } from '@/api/evaluations'
import { getApiErrorMessage, getApiStatus } from '@/api/http'
import { getRequestDetail } from '@/api/requests'
import EvaluationForm from '@/components/evaluation/EvaluationForm.vue'
import EvaluationHistory from '@/components/evaluation/EvaluationHistory.vue'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import type { CreatedEvaluationResult, EvaluationRecord } from '@/types/evaluation'
import type { RequestDetail, RequestUrgency } from '@/types/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const detail = ref<RequestDetail | null>(null)
const evaluations = ref<EvaluationRecord[]>([])
const errorMessage = ref('')
const confirmingEvaluationId = ref<string | null>(null)

let loadSequence = 0

const urgencyMap: Record<RequestUrgency, string> = {
  NORMAL: '一般',
  HIGH: '较急',
  URGENT: '紧急',
}

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const moneyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
})

function formatDateTime(value: string | null): string {
  if (!value) return '—'

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

function formatBudget(value: number | null): string {
  return value === null ? '未填写' : moneyFormatter.format(value)
}

const latestEvaluation = computed<EvaluationRecord | null>(() =>
  evaluations.value.reduce<EvaluationRecord | null>(
    (latest, current) => (latest === null || current.version > latest.version ? current : latest),
    null,
  ),
)

const pendingRejection = computed(
  () =>
    detail.value?.status === 'PENDING_REVIEW' &&
    latestEvaluation.value?.conclusion === 'NOT_FEASIBLE',
)

const isTeamMember = computed(
  () => authStore.user?.role === 'MEMBER' || authStore.user?.role === 'ADMIN',
)

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

const canEvaluate = computed(
  () => isTeamMember.value && detail.value?.status === 'PENDING_REVIEW' && !pendingRejection.value,
)

const confirmableEvaluationId = computed(() =>
  isAdmin.value && pendingRejection.value ? (latestEvaluation.value?.id ?? null) : null,
)

async function loadPage() {
  const id = String(route.params.id ?? '')
  const sequence = ++loadSequence

  detail.value = null
  evaluations.value = []
  errorMessage.value = ''

  if (!/^[1-9]\d*$/.test(id)) {
    errorMessage.value = '需求编号格式不正确'
    return
  }

  loading.value = true

  try {
    const [requestDetail, evaluationHistory] = await Promise.all([
      getRequestDetail(id),
      getEvaluations(id),
    ])

    if (sequence !== loadSequence) return

    detail.value = requestDetail
    evaluations.value = evaluationHistory
  } catch (error) {
    if (sequence === loadSequence) {
      errorMessage.value = getApiErrorMessage(error, '需求不存在、已被删除，或当前账号没有查看权限')
    }
  } finally {
    if (sequence === loadSequence) {
      loading.value = false
    }
  }
}

function backToList() {
  void router.push(
    route.query.from === 'workspace' ? { name: 'workspace' } : { name: 'request-list' },
  )
}

async function handleSubmitted(result: CreatedEvaluationResult) {
  ElMessage.success(
    result.adminConfirmationRequired ? '评估已提交，等待管理员确认不承接结论' : '评估提交成功',
  )

  await loadPage()
}

async function handleConflict() {
  await loadPage()
}

async function handleConfirmRejection(evaluation: EvaluationRecord) {
  if (!detail.value || confirmingEvaluationId.value !== null) {
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认后需求将进入“已驳回”，该操作会影响需求方。是否继续？',
      '确认不承接',
      {
        type: 'warning',
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  confirmingEvaluationId.value = evaluation.id

  try {
    await confirmEvaluationRejection(detail.value.id, evaluation.id, detail.value.version)

    ElMessage.success('已确认不承接，需求已驳回')
    await loadPage()
  } catch (error) {
    if (getApiStatus(error) === 409) {
      ElMessage.warning('需求已被其他成员更新，正在重新加载最新数据')
      await loadPage()
    } else {
      ElMessage.error(getApiErrorMessage(error, '确认驳回失败，请稍后重试'))
    }
  } finally {
    confirmingEvaluationId.value = null
  }
}

watch(
  () => route.params.id,
  () => void loadPage(),
  { immediate: true },
)
</script>

<template>
  <section class="page">
    <el-skeleton v-if="loading" :rows="8" animated />

    <el-result
      v-else-if="errorMessage"
      icon="warning"
      title="无法查看需求"
      :sub-title="errorMessage"
    >
      <template #extra>
        <el-button type="primary" @click="backToList">返回需求列表</el-button>
        <el-button @click="loadPage">重新加载</el-button>
      </template>
    </el-result>

    <template v-else-if="detail">
      <div class="page__header">
        <div>
          <h2>{{ detail.title }}</h2>
          <span class="request-no">{{ detail.requestNo ?? '尚未生成编号' }}</span>
        </div>
        <div class="header-actions">
          <RequestStatusTag :status="detail.status" />
          <el-button @click="backToList">返回列表</el-button>
        </div>
      </div>

      <el-card>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="需求分类">{{ detail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detail.creatorName }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            {{ urgencyMap[detail.urgency] }}
          </el-descriptions-item>
          <el-descriptions-item label="期望完成日期">
            {{ detail.expectedDeadline }}
          </el-descriptions-item>
          <el-descriptions-item label="预算金额">
            {{ formatBudget(detail.budgetAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="预算说明">
            {{ detail.budgetDescription ?? '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(detail.submittedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(detail.updatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理进度" :span="2">
            <el-progress :percentage="detail.progress" />
          </el-descriptions-item>
          <el-descriptions-item label="联系方式" :span="2">
            {{ detail.contactInfo ?? '当前账号无权查看' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card header="需求背景"
        ><div class="text-content">{{ detail.background }}</div></el-card
      >
      <el-card header="具体需求"
        ><div class="text-content">{{ detail.description }}</div></el-card
      >
      <el-card header="期望成果"
        ><div class="text-content">{{ detail.expectedResult }}</div></el-card
      >
      <el-card header="技术限制">
        <div class="text-content">{{ detail.technicalConstraints ?? '未填写' }}</div>
      </el-card>
      <EvaluationHistory
        :evaluations="evaluations"
        :confirmable-evaluation-id="confirmableEvaluationId"
        :confirming-evaluation-id="confirmingEvaluationId"
        @confirm-rejection="handleConfirmRejection"
      />

      <el-alert
        v-if="pendingRejection && !isAdmin"
        type="warning"
        :closable="false"
        title="最新的不承接结论正在等待管理员确认"
      />

      <EvaluationForm
        v-if="canEvaluate && detail"
        :key="`${detail.id}-${detail.version}`"
        :request-id="detail.id"
        :request-version="detail.version"
        @submitted="handleSubmitted"
        @conflict="handleConflict"
      />
      <el-card header="状态历史">
        <el-empty v-if="detail.statusHistory.length === 0" description="暂无状态记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="history in detail.statusHistory"
            :key="history.id"
            :timestamp="formatDateTime(history.createdAt)"
          >
            <div class="history-title">
              <RequestStatusTag :status="history.toStatus" />
              <span>{{ history.operatorName }}</span>
            </div>
            <p>{{ history.reason ?? '无补充说明' }}</p>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>
  </section>
</template>

<style scoped>
.request-no {
  color: #6b7280;
}

.header-actions,
.history-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.text-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.8;
}
</style>

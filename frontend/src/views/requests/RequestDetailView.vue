<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRequestAssignment } from '@/api/assignments'
import { getRequestProgress } from '@/api/progress'
import { getDeliveryAcceptance } from '@/api/deliveries'
import AssignmentPanel from '@/components/assignment/AssignmentPanel.vue'
import DeliveryAcceptancePanel from '@/components/delivery/DeliveryAcceptancePanel.vue'
import ProgressPanel from '@/components/progress/ProgressPanel.vue'
import RequestTimeline from '@/components/progress/RequestTimeline.vue'
import type { RequestAssignment } from '@/types/assignment'
import type { RequestProgressSnapshot } from '@/types/progress'
import type { DeliveryAcceptanceSnapshot } from '@/types/delivery'
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
const assignment = ref<RequestAssignment | null>(null)
const progressSnapshot = ref<RequestProgressSnapshot | null>(null)
const deliveryAcceptanceSnapshot = ref<DeliveryAcceptanceSnapshot | null>(null)
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

async function loadPage(retryCount = 0) {
  const id = String(route.params.id ?? '')
  const sequence = ++loadSequence

  /*
   * 每次重新加载时先清空旧数据。
   *
   * 特别是 assignment，不能保留上一次请求的成员配置，
   * 否则路由切换或者 409 冲突重新加载期间可能短暂显示旧数据。
   */
  detail.value = null
  evaluations.value = []
  assignment.value = null
  progressSnapshot.value = null
  deliveryAcceptanceSnapshot.value = null
  errorMessage.value = ''

  if (!/^[1-9]\d*$/.test(id)) {
    errorMessage.value = '需求编号格式不正确'
    return
  }

  loading.value = true

  try {
    /*
     * 需求详情、评估历史和任务成员配置并行加载。
     *
     * assignment 中的 requestVersion 必须以服务端返回结果为准，
     * 前端不能自行推算。
     */
    const [
      requestDetail,
      evaluationHistory,
      requestAssignment,
      requestProgress,
      deliveryAcceptance,
    ] = await Promise.all([
      getRequestDetail(id),
      getEvaluations(id),
      getRequestAssignment(id),
      getRequestProgress(id),
      getDeliveryAcceptance(id),
    ])

    const versions = [
      requestDetail.version,
      requestAssignment.requestVersion,
      requestProgress.requestVersion,
      deliveryAcceptance.requestVersion,
    ]
    if (new Set(versions).size > 1) {
      if (retryCount < 1) {
        await loadPage(retryCount + 1)
        return
      }
      if (sequence === loadSequence) {
        errorMessage.value = '需求刚刚发生变化，请点击重新加载获取一致数据'
      }
      return
    }

    /*
     * 如果在请求过程中已经触发了下一次 loadPage，
     * 当前这一批旧请求结果直接丢弃。
     */
    if (sequence !== loadSequence) return

    detail.value = requestDetail
    evaluations.value = evaluationHistory
    assignment.value = requestAssignment
    progressSnapshot.value = requestProgress
    deliveryAcceptanceSnapshot.value = deliveryAcceptance
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

  /*
   * 评估成功以后重新从服务端读取整个页面。
   *
   * 不在前端自行修改 detail.version / detail.status。
   */
  await loadPage()
}

async function handleConflict() {
  /*
   * 评估发生并发冲突时重新读取服务器最新状态。
   */
  await loadPage()
}

/*
 * AssignmentPanel 成功更新任务成员以后触发。
 *
 * 必须重新调用 loadPage()：
 *
 * - 重新读取 request.status
 * - 重新读取 request.version
 * - 重新读取 assignment.requestVersion
 * - 重新读取负责人
 * - 重新读取参与成员
 * - 重新读取 status_history
 *
 * 前端不能自行执行 version + 1。
 */
async function handleAssignmentUpdated() {
  ElMessage.success('任务成员已更新')
  await loadPage()
}

/*
 * AssignmentPanel 请求出现 409 时触发。
 *
 * 409 表示服务端 CAS 失败或者成员关系已经被其他管理员修改。
 * 此时不能继续使用当前页面中的旧 requestVersion。
 */
async function handleAssignmentConflict() {
  ElMessage.warning('任务成员已被其他管理员更新，正在重新加载')
  await loadPage()
}

async function handleProgressUpdated(): Promise<void> {
  ElMessage.success('进度记录已发布')
  await loadPage()
}

async function handleProgressConflict(): Promise<void> {
  await loadPage()
}

async function handleDeliveryAcceptanceUpdated(): Promise<void> {
  ElMessage.success('交付或验收操作已完成')
  await loadPage()
}

async function handleDeliveryAcceptanceConflict(): Promise<void> {
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

    /*
     * 成功后重新读取服务端状态。
     */
    await loadPage()
  } catch (error) {
    if (getApiStatus(error) === 409) {
      ElMessage.warning('需求已被其他成员更新，正在重新加载最新数据')

      /*
       * 409 后同样重新读取服务端状态。
       */
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
        <el-button type="primary" @click="backToList"> 返回需求列表 </el-button>

        <el-button @click="loadPage"> 重新加载 </el-button>
      </template>
    </el-result>

    <template v-else-if="detail">
      <!-- ===================================================== -->
      <!-- 页面头部 -->
      <!-- ===================================================== -->

      <div class="page__header">
        <div>
          <h2>{{ detail.title }}</h2>
          <span class="request-no">
            {{ detail.requestNo ?? '尚未生成编号' }}
          </span>
        </div>

        <div class="header-actions">
          <RequestStatusTag :status="detail.status" />

          <el-button @click="backToList"> 返回列表 </el-button>
        </div>
      </div>

      <!-- ===================================================== -->
      <!-- 需求基础信息 -->
      <!-- ===================================================== -->

      <el-card>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="需求分类">
            {{ detail.categoryName }}
          </el-descriptions-item>

          <el-descriptions-item label="创建人">
            {{ detail.creatorName }}
          </el-descriptions-item>

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

      <!-- ===================================================== -->
      <!-- 需求背景 -->
      <!-- ===================================================== -->

      <el-card header="需求背景">
        <div class="text-content">
          {{ detail.background }}
        </div>
      </el-card>

      <!-- ===================================================== -->
      <!-- 具体需求 -->
      <!-- ===================================================== -->

      <el-card header="具体需求">
        <div class="text-content">
          {{ detail.description }}
        </div>
      </el-card>

      <!-- ===================================================== -->
      <!-- 期望成果 -->
      <!-- ===================================================== -->

      <el-card header="期望成果">
        <div class="text-content">
          {{ detail.expectedResult }}
        </div>
      </el-card>

      <!-- ===================================================== -->
      <!-- 技术限制 -->
      <!-- ===================================================== -->

      <el-card header="技术限制">
        <div class="text-content">
          {{ detail.technicalConstraints ?? '未填写' }}
        </div>
      </el-card>

      <!-- ===================================================== -->
      <!-- 评估历史 -->
      <!-- ===================================================== -->

      <EvaluationHistory
        :evaluations="evaluations"
        :confirmable-evaluation-id="confirmableEvaluationId"
        :confirming-evaluation-id="confirmingEvaluationId"
        @confirm-rejection="handleConfirmRejection"
      />

      <!-- ===================================================== -->
      <!-- 等待管理员确认不承接 -->
      <!-- ===================================================== -->

      <el-alert
        v-if="pendingRejection && !isAdmin"
        type="warning"
        :closable="false"
        title="最新的不承接结论正在等待管理员确认"
      />

      <!-- ===================================================== -->
      <!-- 新建评估 -->
      <!-- ===================================================== -->

      <EvaluationForm
        v-if="canEvaluate && detail"
        :key="`${detail.id}-${detail.version}`"
        :request-id="detail.id"
        :request-version="detail.version"
        @submitted="handleSubmitted"
        @conflict="handleConflict"
      />

      <!-- ===================================================== -->
      <!-- 任务成员 -->
      <!--
          必须放在评估区域之后、状态历史之前。

          key 使用服务端返回的 requestVersion。

          update 成功：
              AssignmentPanel
                  ↓
              updated
                  ↓
              handleAssignmentUpdated()
                  ↓
              loadPage()

          409：
              AssignmentPanel
                  ↓
              conflict
                  ↓
              handleAssignmentConflict()
                  ↓
              loadPage()

          两种情况下都禁止前端自行推算 requestVersion。
      -->
      <!-- ===================================================== -->

      <AssignmentPanel
        v-if="assignment"
        :key="assignment.requestVersion"
        :assignment="assignment"
        :is-admin="isAdmin"
        @updated="handleAssignmentUpdated"
        @conflict="handleAssignmentConflict"
      />

      <ProgressPanel
        v-if="progressSnapshot"
        :key="progressSnapshot.requestVersion"
        :snapshot="progressSnapshot"
        @updated="handleProgressUpdated"
        @conflict="handleProgressConflict"
      />

      <DeliveryAcceptancePanel
        v-if="deliveryAcceptanceSnapshot"
        :key="deliveryAcceptanceSnapshot.requestVersion"
        :snapshot="deliveryAcceptanceSnapshot"
        @updated="handleDeliveryAcceptanceUpdated"
        @conflict="handleDeliveryAcceptanceConflict"
      />

      <RequestTimeline
        :status-history="detail.statusHistory"
        :progress-logs="progressSnapshot?.logs ?? []"
        :deliveries="deliveryAcceptanceSnapshot?.deliveries ?? []"
        :acceptances="deliveryAcceptanceSnapshot?.acceptances ?? []"
      />
    </template>
  </section>
</template>

<style scoped>
.request-no {
  color: #6b7280;
}

.header-actions {
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

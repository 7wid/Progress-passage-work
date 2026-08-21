<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  CircleX,
  FileArchive,
  FileText,
  ListTodo,
  Pencil,
  ScrollText,
} from '@lucide/vue'
import { getRequestAssignment } from '@/api/assignments'
import { getRequestProgress } from '@/api/progress'
import { getDeliveryAcceptance } from '@/api/deliveries'
import { getRequestAttachments } from '@/api/attachments'
import AdminRequestActions from '@/components/admin/AdminRequestActions.vue'
import AssignmentPanel from '@/components/assignment/AssignmentPanel.vue'
import AttachmentList from '@/components/common/AttachmentList.vue'
import AttachmentUploader from '@/components/common/AttachmentUploader.vue'
import AppPageHeader from '@/components/common/AppPageHeader.vue'
import DeliveryAcceptancePanel from '@/components/delivery/DeliveryAcceptancePanel.vue'
import ProgressPanel from '@/components/progress/ProgressPanel.vue'
import RequestTimeline from '@/components/progress/RequestTimeline.vue'
import type { RequestAssignment } from '@/types/assignment'
import type { RequestProgressSnapshot } from '@/types/progress'
import type { DeliveryAcceptanceSnapshot } from '@/types/delivery'
import type { AttachmentSnapshot } from '@/types/attachment'
import { useRoute, useRouter } from 'vue-router'
import { confirmEvaluationRejection, getEvaluations } from '@/api/evaluations'
import { getApiErrorMessage, getApiStatus } from '@/api/http'
import { cancelRequest, getRequestDetail } from '@/api/requests'
import EvaluationForm from '@/components/evaluation/EvaluationForm.vue'
import EvaluationHistory from '@/components/evaluation/EvaluationHistory.vue'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import RequesterJourneyPanel from '@/components/requests/RequesterJourneyPanel.vue'
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
const requestAttachmentSnapshot = ref<AttachmentSnapshot | null>(null)
const pendingDeliveryAttachmentSnapshot = ref<AttachmentSnapshot | null>(null)
const requestAttachmentError = ref('')
const pendingDeliveryAttachmentError = ref('')
const pendingDeliveryAttachmentLoading = ref(false)
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

const isRequester = computed(() => authStore.user?.role === 'REQUESTER')

const isCreator = computed(
  () => detail.value !== null && authStore.user?.id === detail.value.creatorId,
)

const canEdit = computed(
  () =>
    isCreator.value &&
    (detail.value?.status === 'DRAFT' || detail.value?.status === 'NEED_MORE_INFO'),
)

const canCancel = computed(
  () =>
    isCreator.value &&
    detail.value !== null &&
    ['DRAFT', 'PENDING_REVIEW', 'NEED_MORE_INFO', 'PENDING_ASSIGNMENT'].includes(
      detail.value.status,
    ),
)

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
  requestAttachmentSnapshot.value = null
  pendingDeliveryAttachmentSnapshot.value = null
  requestAttachmentError.value = ''
  pendingDeliveryAttachmentError.value = ''
  pendingDeliveryAttachmentLoading.value = false
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

    const [requestAttachmentsResult, pendingDeliveryAttachmentsResult] = await Promise.allSettled([
      getRequestAttachments(id, 'REQUEST'),
      getRequestAttachments(id, 'DELIVERY', true),
    ])
    if (sequence !== loadSequence) return

    if (requestAttachmentsResult.status === 'fulfilled') {
      requestAttachmentSnapshot.value = requestAttachmentsResult.value
    } else {
      requestAttachmentError.value = getApiErrorMessage(
        requestAttachmentsResult.reason,
        '需求附件加载失败',
      )
    }
    if (pendingDeliveryAttachmentsResult.status === 'fulfilled') {
      pendingDeliveryAttachmentSnapshot.value = pendingDeliveryAttachmentsResult.value
    } else {
      pendingDeliveryAttachmentError.value = getApiErrorMessage(
        pendingDeliveryAttachmentsResult.reason,
        '待提交交付附件加载失败',
      )
    }
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

async function reloadRequestAttachments(): Promise<void> {
  if (!detail.value) return
  requestAttachmentError.value = ''
  try {
    requestAttachmentSnapshot.value = await getRequestAttachments(detail.value.id, 'REQUEST')
  } catch (error) {
    requestAttachmentError.value = getApiErrorMessage(error, '需求附件加载失败')
  }
}

async function reloadPendingDeliveryAttachments(): Promise<void> {
  if (!detail.value) return
  pendingDeliveryAttachmentError.value = ''
  pendingDeliveryAttachmentLoading.value = true
  try {
    pendingDeliveryAttachmentSnapshot.value = await getRequestAttachments(
      detail.value.id,
      'DELIVERY',
      true,
    )
  } catch (error) {
    pendingDeliveryAttachmentError.value = getApiErrorMessage(error, '待提交交付附件加载失败')
  } finally {
    pendingDeliveryAttachmentLoading.value = false
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

async function handleAdminRequestUpdated(): Promise<void> {
  await loadPage()
}

async function handleAdminRequestConflict(): Promise<void> {
  await loadPage()
}

function editRequest(): void {
  if (!detail.value) return
  void router.push({ name: 'request-edit', params: { id: detail.value.id } })
}

async function handleCancelRequest(): Promise<void> {
  if (!detail.value || !canCancel.value) return
  try {
    const result = await ElMessageBox.prompt(
      detail.value.status === 'DRAFT' ? '请填写放弃草稿的原因' : '请填写取消需求的原因',
      detail.value.status === 'DRAFT' ? '放弃草稿' : '取消需求',
      {
        type: 'warning',
        confirmButtonText: '确认取消',
        cancelButtonText: '返回',
        inputValidator: (value) => {
          const length = value.trim().length
          return length >= 5 && length <= 500 ? true : '原因应为 5～500 个字符'
        },
      },
    )
    await cancelRequest(detail.value.id, detail.value.version, result.value)
    ElMessage.success('需求已取消')
    await loadPage()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (getApiStatus(error) === 409) {
      ElMessage.warning('需求状态或版本已变化，正在重新加载')
      await loadPage()
      return
    }
    ElMessage.error(getApiErrorMessage(error, '取消需求失败'))
  }
}

async function handleConfirmRejection(evaluation: EvaluationRecord) {
  if (!detail.value || confirmingEvaluationId.value !== null) {
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认后需求将进入“已驳回”，申请人会收到结果通知。是否继续？',
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
      <AppPageHeader
        :title="detail.title || '未命名需求'"
        description="查看需求信息、协作记录与当前处理状态。"
        eyebrow="REQUEST DETAIL"
        :icon="FileText"
      >
        <template #meta>
          <span class="request-no">{{ detail.requestNo ?? '尚未生成编号' }}</span>
          <RequestStatusTag :status="detail.status" />
        </template>
        <template #actions>
          <el-button v-if="canEdit" type="primary" @click="editRequest">
            <Pencil :size="16" aria-hidden="true" />
            {{ detail.status === 'NEED_MORE_INFO' ? '补充资料' : '编辑草稿' }}
          </el-button>
          <el-button v-if="canCancel" type="danger" plain @click="handleCancelRequest">
            <CircleX :size="16" aria-hidden="true" />
            {{ detail.status === 'DRAFT' ? '放弃草稿' : '取消需求' }}
          </el-button>
          <el-button @click="backToList">
            <ArrowLeft :size="16" aria-hidden="true" />返回列表
          </el-button>
        </template>
      </AppPageHeader>

      <RequesterJourneyPanel v-if="isRequester && isCreator" :status="detail.status" />

      <el-card class="overview-card">
        <template #header>
          <div class="detail-section-heading">
            <span aria-hidden="true"><ListTodo :size="18" /></span>
            <div><strong>需求概览</strong><small>基础属性与处理进度</small></div>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="需求分类">
            {{ detail.categoryName }}
          </el-descriptions-item>

          <el-descriptions-item label="申请人">
            {{ detail.creatorName }}
          </el-descriptions-item>

          <el-descriptions-item label="紧急程度">
            {{ detail.urgency ? urgencyMap[detail.urgency] : '未选择' }}
          </el-descriptions-item>

          <el-descriptions-item label="期望完成日期">
            {{ detail.expectedDeadline ?? '未填写' }}
          </el-descriptions-item>

          <el-descriptions-item label="预算金额">
            {{ formatBudget(detail.budgetAmount) }}
          </el-descriptions-item>

          <el-descriptions-item label="预算说明">
            {{ detail.budgetDescription ?? '未填写' }}
          </el-descriptions-item>

          <el-descriptions-item label="发起时间">
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

      <el-card class="request-content-card">
        <template #header>
          <div class="detail-section-heading">
            <span aria-hidden="true"><ScrollText :size="18" /></span>
            <div><strong>需求说明</strong><small>背景、目标与实施约束</small></div>
          </div>
        </template>
        <div class="request-content-grid">
          <section>
            <h2>需求背景</h2>
            <div class="text-content">{{ detail.background ?? '未填写' }}</div>
          </section>
          <section>
            <h2>具体需求</h2>
            <div class="text-content">{{ detail.description ?? '未填写' }}</div>
          </section>
          <section>
            <h2>期望成果</h2>
            <div class="text-content">{{ detail.expectedResult ?? '未填写' }}</div>
          </section>
          <section>
            <h2>实施约束</h2>
            <div class="text-content">{{ detail.technicalConstraints ?? '未填写' }}</div>
          </section>
        </div>
      </el-card>

      <el-card>
        <template #header>
          <div class="detail-section-heading">
            <span class="detail-section-heading__icon--orange" aria-hidden="true">
              <FileArchive :size="18" />
            </span>
            <div><strong>需求附件</strong><small>申请人提供的参考资料</small></div>
          </div>
        </template>
        <el-alert
          v-if="requestAttachmentError"
          type="warning"
          :closable="false"
          :title="requestAttachmentError"
        >
          <template #default>
            <el-button link type="primary" @click="reloadRequestAttachments">重试加载</el-button>
          </template>
        </el-alert>
        <AttachmentUploader
          v-if="requestAttachmentSnapshot?.canUpload"
          :model-value="requestAttachmentSnapshot.attachments"
          :request-id="detail.id"
          business-type="REQUEST"
          @update:model-value="
            requestAttachmentSnapshot && (requestAttachmentSnapshot.attachments = $event)
          "
        />
        <AttachmentList
          v-else
          :attachments="requestAttachmentSnapshot?.attachments ?? []"
          empty-description="暂无需求附件"
        />
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
        :pending-attachments="pendingDeliveryAttachmentSnapshot?.attachments ?? []"
        :pending-attachments-ready="
          pendingDeliveryAttachmentSnapshot !== null &&
          pendingDeliveryAttachmentError === '' &&
          !pendingDeliveryAttachmentLoading
        "
        @updated="handleDeliveryAcceptanceUpdated"
        @conflict="handleDeliveryAcceptanceConflict"
      />

      <AdminRequestActions
        v-if="isAdmin"
        :key="`admin-actions-${detail.version}`"
        :request-id="detail.id"
        :status="detail.status"
        :version="detail.version"
        @updated="handleAdminRequestUpdated"
        @conflict="handleAdminRequestConflict"
      />

      <el-alert
        v-if="pendingDeliveryAttachmentError && deliveryAcceptanceSnapshot?.canSubmitDelivery"
        type="warning"
        :closable="false"
        :title="pendingDeliveryAttachmentError"
      >
        <template #default>
          <el-button link type="primary" @click="reloadPendingDeliveryAttachments">
            重试加载待提交附件
          </el-button>
        </template>
      </el-alert>

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
  padding: 3px 8px;
  color: var(--color-text-secondary);
  background: var(--color-surface-secondary);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  font-family: SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 11px;
}

.detail-section-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-section-heading > span {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-radius: var(--radius-md);
}

.detail-section-heading > span.detail-section-heading__icon--orange {
  color: var(--color-warning);
  background: #fff7ed;
}

.detail-section-heading > div {
  display: grid;
  gap: 1px;
}

.detail-section-heading strong {
  color: var(--color-text-primary);
  font-size: 15px;
}

.detail-section-heading small {
  color: var(--color-text-tertiary);
  font-size: 12px;
  font-weight: 500;
}

.request-content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.request-content-grid section {
  min-width: 0;
  padding: 2px 22px 22px 0;
}

.request-content-grid section:nth-child(even) {
  padding-right: 0;
  padding-left: 22px;
  border-left: 1px solid var(--color-border-subtle);
}

.request-content-grid section:nth-child(n + 3) {
  padding-top: 22px;
  padding-bottom: 2px;
  border-top: 1px solid var(--color-border-subtle);
}

.request-content-grid h2 {
  margin: 0 0 8px;
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 650;
}

.text-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

@media (max-width: 700px) {
  .request-content-grid {
    grid-template-columns: 1fr;
  }

  .request-content-grid section,
  .request-content-grid section:nth-child(even),
  .request-content-grid section:nth-child(n + 3) {
    padding: 18px 0;
    border-top: 1px solid var(--color-border-subtle);
    border-left: 0;
  }

  .request-content-grid section:first-child {
    padding-top: 0;
    border-top: 0;
  }

  .request-content-grid section:last-child {
    padding-bottom: 0;
  }
}
</style>

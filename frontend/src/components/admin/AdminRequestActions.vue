<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleX, RotateCcw, ShieldAlert } from '@lucide/vue'
import { cancelRequestAsAdmin, reopenRequestAsAdmin } from '@/api/adminRequests'
import { getApiErrorMessage, getApiFieldErrors, getApiStatus } from '@/api/http'
import AdminReasonDialog from '@/components/admin/AdminReasonDialog.vue'
import AppSectionHeader from '@/components/common/AppSectionHeader.vue'
import type { AdminRequestActionResult } from '@/types/admin'
import type { RequestStatus } from '@/types/request'

type RequestAdminAction = 'CANCEL' | 'REOPEN'

const props = defineProps<{
  requestId: string
  status: RequestStatus
  version: number
}>()

const emit = defineEmits<{
  updated: [result: AdminRequestActionResult]
  conflict: []
}>()

const cancellableStatuses = new Set<RequestStatus>([
  'PENDING_REVIEW',
  'NEED_MORE_INFO',
  'PENDING_ASSIGNMENT',
  'IN_PROGRESS',
  'PENDING_ACCEPTANCE',
])
const reopenableStatuses = new Set<RequestStatus>(['REJECTED', 'CANCELLED', 'COMPLETED'])

const activeAction = ref<RequestAdminAction | null>(null)
const dialogVisible = ref(false)
const submitting = ref(false)
const reasonError = ref('')

const canCancel = computed(() => cancellableStatuses.has(props.status))
const canReopen = computed(() => reopenableStatuses.has(props.status))
const hasAction = computed(() => canCancel.value || canReopen.value)
const dialogTitle = computed(() =>
  activeAction.value === 'CANCEL' ? '管理员取消需求' : '重新开启需求',
)
const dialogDescription = computed(() =>
  activeAction.value === 'CANCEL'
    ? '取消后需求将进入“已取消”，并通知相关人员。请确认已经完成必要的线下沟通。'
    : '系统会根据历史状态和当前负责人恢复到安全的处理状态，请确认确实需要重新开启。',
)

function openAction(action: RequestAdminAction): void {
  if (submitting.value) return
  activeAction.value = action
  reasonError.value = ''
  dialogVisible.value = true
}

async function submitAction(reason: string): Promise<void> {
  const action = activeAction.value
  if (!action || submitting.value) return
  submitting.value = true
  reasonError.value = ''
  try {
    const result =
      action === 'CANCEL'
        ? await cancelRequestAsAdmin(props.requestId, {
            expectedVersion: props.version,
            reason,
          })
        : await reopenRequestAsAdmin(props.requestId, {
            expectedVersion: props.version,
            reason,
          })
    dialogVisible.value = false
    activeAction.value = null
    ElMessage.success(action === 'CANCEL' ? '需求已由管理员取消' : '需求已重新开启')
    emit('updated', result)
  } catch (error) {
    if (getApiStatus(error) === 409) {
      dialogVisible.value = false
      activeAction.value = null
      ElMessage.warning('需求状态或版本已经变化，正在重新加载最新数据')
      emit('conflict')
      return
    }
    const fields = getApiFieldErrors(error)
    reasonError.value = fields.reason ?? ''
    ElMessage.error(getApiErrorMessage(error, '管理员需求操作失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-card v-if="hasAction" class="admin-actions">
    <template #header>
      <AppSectionHeader
        title="管理员异常处理"
        description="仅用于已确认的异常业务状态"
        :icon="ShieldAlert"
        tone="red"
      />
    </template>
    <el-alert
      type="warning"
      :closable="false"
      title="以下操作会改变业务状态，并记录操作人和原因。请仅在异常处理或线下确认后使用。"
      show-icon
    />

    <div class="action-buttons">
      <el-button v-if="canCancel" type="danger" plain @click="openAction('CANCEL')">
        <CircleX :size="16" aria-hidden="true" />
        管理员取消需求
      </el-button>
      <el-button v-if="canReopen" type="primary" plain @click="openAction('REOPEN')">
        <RotateCcw :size="16" aria-hidden="true" />
        重新开启需求
      </el-button>
    </div>

    <AdminReasonDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :description="dialogDescription"
      :confirm-text="activeAction === 'CANCEL' ? '确认取消需求' : '确认重新开启'"
      :danger="activeAction === 'CANCEL'"
      :submitting="submitting"
      :server-error="reasonError"
      @confirm="submitAction"
    />
  </el-card>
</template>

<style scoped>
.admin-actions {
  border-color: #f3d19e;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.action-buttons :deep(.el-button) {
  margin: 0;
}

.action-buttons :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}
</style>

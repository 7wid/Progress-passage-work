<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createAcceptance, createDelivery } from '@/api/deliveries'
import { getApiErrorMessage, getApiFieldErrors, getApiStatus } from '@/api/http'
import AttachmentList from '@/components/common/AttachmentList.vue'
import AttachmentUploader from '@/components/common/AttachmentUploader.vue'
import type { AttachmentRecord } from '@/types/attachment'
import type {
  AcceptanceResult,
  CreatedAcceptanceResult,
  CreatedDeliveryResult,
  DeliveryAcceptanceSnapshot,
  DeliveryRecord,
} from '@/types/delivery'

const props = withDefaults(
  defineProps<{
    snapshot: DeliveryAcceptanceSnapshot
    pendingAttachments?: AttachmentRecord[]
    pendingAttachmentsReady?: boolean
  }>(),
  {
    pendingAttachments: () => [],
    pendingAttachmentsReady: true,
  },
)
const emit = defineEmits<{
  updated: [result: CreatedDeliveryResult | CreatedAcceptanceResult]
  conflict: []
}>()

const deliveryFormRef = ref<FormInstance>()
const acceptanceFormRef = ref<FormInstance>()
const submittingAction = ref<'DELIVERY' | 'ACCEPTANCE' | null>(null)
const deliveryServerErrors = reactive<Record<string, string>>({})
const acceptanceServerErrors = reactive<Record<string, string>>({})
const deliveryForm = reactive({ description: '', deliveryUrl: '' })
const deliveryAttachments = ref<AttachmentRecord[]>([])
const uploadingAttachments = ref(false)
const acceptanceForm = reactive<{ result: AcceptanceResult; comment: string }>({
  result: 'ACCEPTED',
  comment: '',
})

// 后端按 created_at DESC, id DESC 稳定排序，验收操作也绑定该列表的第一条。
const latestDelivery = computed<DeliveryRecord | null>(() => props.snapshot.deliveries[0] ?? null)

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatDateTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

function normalizedHttpUrl(value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  try {
    const url = new URL(trimmed)
    const allowedProtocol = url.protocol === 'http:' || url.protocol === 'https:'
    const safeAuthority = Boolean(url.hostname) && !url.username && !url.password
    return allowedProtocol && safeAuthority ? trimmed : null
  } catch {
    return null
  }
}

const deliveryRules: FormRules = {
  description: [
    {
      validator: (_rule, _value, callback) => {
        const length = deliveryForm.description.trim().length
        if (length < 5 || length > 5000) {
          callback(new Error('交付说明应为 5～5000 个字符'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  deliveryUrl: [
    {
      validator: (_rule, _value, callback) => {
        const value = deliveryForm.deliveryUrl.trim()
        if (value.length > 1000) return callback(new Error('交付地址不能超过 1000 个字符'))
        if (value && normalizedHttpUrl(value) === null) {
          return callback(new Error('交付地址必须是完整的 http 或 https 链接'))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const acceptanceRules: FormRules = {
  result: [{ required: true, message: '请选择验收结果', trigger: 'change' }],
  comment: [
    {
      validator: (_rule, _value, callback) => {
        const length = acceptanceForm.comment.trim().length
        if (length > 2000) return callback(new Error('验收评价不能超过 2000 个字符'))
        if (acceptanceForm.result === 'REWORK_REQUIRED' && length < 5) {
          return callback(new Error('退回修改时必须填写 5～2000 个字符的修改意见'))
        }
        callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
}

function clearErrors(errors: Record<string, string>): void {
  Object.keys(errors).forEach((key) => delete errors[key])
}

function resetForms(): void {
  deliveryForm.description = ''
  deliveryForm.deliveryUrl = ''
  acceptanceForm.result = 'ACCEPTED'
  acceptanceForm.comment = ''
  clearErrors(deliveryServerErrors)
  clearErrors(acceptanceServerErrors)
  deliveryFormRef.value?.clearValidate()
  acceptanceFormRef.value?.clearValidate()
}

async function confirmAction(action: 'DELIVERY' | 'ACCEPTANCE'): Promise<boolean> {
  const accepting = action === 'ACCEPTANCE' && acceptanceForm.result === 'ACCEPTED'
  const message =
    action === 'DELIVERY'
      ? '提交后需求将进入待验收状态，交付记录不可修改。是否继续？'
      : accepting
        ? '验收通过后需求将正式完成。是否确认通过？'
        : '退回后需求将重新进入处理中。是否确认退回修改？'
  try {
    await ElMessageBox.confirm(message, action === 'DELIVERY' ? '确认提交交付' : '确认验收结论', {
      type: accepting ? 'success' : 'warning',
      confirmButtonText: '确认提交',
      cancelButtonText: '返回修改',
    })
    return true
  } catch {
    return false
  }
}

function handleSubmitError(error: unknown, errors: Record<string, string>, fallback: string): void {
  if (getApiStatus(error) === 409) {
    ElMessage.warning('需求已被其他用户更新，正在重新加载最新数据')
    emit('conflict')
    return
  }
  Object.assign(errors, getApiFieldErrors(error))
  ElMessage.error(getApiErrorMessage(error, fallback))
}

async function handleDeliverySubmit(): Promise<void> {
  if (
    !deliveryFormRef.value ||
    submittingAction.value ||
    uploadingAttachments.value ||
    !props.pendingAttachmentsReady ||
    !props.snapshot.canSubmitDelivery
  )
    return
  submittingAction.value = 'DELIVERY'
  clearErrors(deliveryServerErrors)
  try {
    const valid = await deliveryFormRef.value.validate().catch(() => false)
    if (!valid || !(await confirmAction('DELIVERY'))) return
    const result = await createDelivery(props.snapshot.requestId, {
      requestVersion: props.snapshot.requestVersion,
      description: deliveryForm.description.trim(),
      deliveryUrl: deliveryForm.deliveryUrl.trim() || null,
      attachmentIds: deliveryAttachments.value.map((attachment) => attachment.id),
    })
    emit('updated', result)
  } catch (error) {
    handleSubmitError(error, deliveryServerErrors, '提交交付失败，请稍后重试')
  } finally {
    submittingAction.value = null
  }
}

async function handleAcceptanceSubmit(): Promise<void> {
  if (!acceptanceFormRef.value || submittingAction.value || !props.snapshot.canAccept) return
  submittingAction.value = 'ACCEPTANCE'
  clearErrors(acceptanceServerErrors)
  try {
    const valid = await acceptanceFormRef.value.validate().catch(() => false)
    if (!valid || !(await confirmAction('ACCEPTANCE'))) return
    const result = await createAcceptance(props.snapshot.requestId, {
      requestVersion: props.snapshot.requestVersion,
      result: acceptanceForm.result,
      comment: acceptanceForm.comment.trim() || null,
    })
    emit('updated', result)
  } catch (error) {
    handleSubmitError(error, acceptanceServerErrors, '提交验收结果失败，请稍后重试')
  } finally {
    submittingAction.value = null
  }
}

watch(() => props.snapshot, resetForms, { immediate: true })
watch(
  () => props.pendingAttachments,
  (attachments) => {
    deliveryAttachments.value = [...attachments]
  },
  { immediate: true },
)
</script>

<template>
  <el-card header="交付与验收">
    <div v-if="latestDelivery" class="latest-delivery">
      <div class="section-heading">
        <strong>最新交付</strong>
        <span
          >{{ latestDelivery.submitterName }} · {{ formatDateTime(latestDelivery.createdAt) }}</span
        >
      </div>
      <p>{{ latestDelivery.description }}</p>
      <a
        v-if="latestDelivery.deliveryUrl && normalizedHttpUrl(latestDelivery.deliveryUrl)"
        :href="normalizedHttpUrl(latestDelivery.deliveryUrl) ?? undefined"
        target="_blank"
        rel="noopener noreferrer"
        >打开交付地址</a
      >
      <AttachmentList
        :attachments="latestDelivery.attachments"
        empty-description="本次交付没有附件"
      />
    </div>
    <el-empty v-else description="尚未提交交付物" />

    <template v-if="snapshot.canSubmitDelivery">
      <el-divider />
      <h3>提交交付物</h3>
      <el-alert
        v-if="!pendingAttachmentsReady"
        type="warning"
        :closable="false"
        title="待提交附件加载失败，暂时不能提交交付，请先在上方重试加载。"
      />
      <el-form
        ref="deliveryFormRef"
        :model="deliveryForm"
        :rules="deliveryRules"
        label-position="top"
        @submit.prevent="handleDeliverySubmit"
      >
        <el-form-item label="交付说明" prop="description" :error="deliveryServerErrors.description">
          <el-input
            v-model="deliveryForm.description"
            type="textarea"
            :rows="4"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          label="交付地址（可选）"
          prop="deliveryUrl"
          :error="deliveryServerErrors.deliveryUrl"
        >
          <el-input
            v-model="deliveryForm.deliveryUrl"
            maxlength="1000"
            placeholder="https://example.com/delivery"
          />
        </el-form-item>
        <el-form-item label="交付附件（可选）">
          <AttachmentUploader
            v-model="deliveryAttachments"
            :request-id="snapshot.requestId"
            business-type="DELIVERY"
            :disabled="submittingAction !== null || !pendingAttachmentsReady"
            @uploading-change="uploadingAttachments = $event"
          />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          title="提交后需求将进入待验收状态，请确认交付地址已开放给需求方。"
        />
        <div class="actions">
          <el-button @click="resetForms">清空</el-button>
          <el-button
            type="primary"
            native-type="submit"
            :loading="submittingAction === 'DELIVERY'"
            :disabled="
              uploadingAttachments || submittingAction !== null || !pendingAttachmentsReady
            "
            >提交交付</el-button
          >
        </div>
      </el-form>
    </template>

    <template v-if="snapshot.deliveries.length > 1">
      <el-divider />
      <el-collapse>
        <el-collapse-item title="查看历史交付记录">
          <article
            v-for="delivery in snapshot.deliveries.slice(1)"
            :key="delivery.id"
            class="history-delivery"
          >
            <div class="section-heading">
              <strong>{{ delivery.submitterName }}</strong>
              <span>{{ formatDateTime(delivery.createdAt) }}</span>
            </div>
            <p>{{ delivery.description }}</p>
            <a
              v-if="delivery.deliveryUrl && normalizedHttpUrl(delivery.deliveryUrl)"
              :href="normalizedHttpUrl(delivery.deliveryUrl) ?? undefined"
              target="_blank"
              rel="noopener noreferrer"
            >
              打开交付地址
            </a>
            <AttachmentList
              :attachments="delivery.attachments"
              empty-description="本次交付没有附件"
            />
          </article>
        </el-collapse-item>
      </el-collapse>
    </template>

    <template v-if="snapshot.canAccept && latestDelivery">
      <el-divider />
      <h3>验收最新交付</h3>
      <el-form
        ref="acceptanceFormRef"
        :model="acceptanceForm"
        :rules="acceptanceRules"
        label-position="top"
        @submit.prevent="handleAcceptanceSubmit"
      >
        <el-form-item label="验收结果" prop="result" :error="acceptanceServerErrors.result">
          <el-radio-group v-model="acceptanceForm.result">
            <el-radio value="ACCEPTED">验收通过</el-radio>
            <el-radio value="REWORK_REQUIRED">退回修改</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          :label="acceptanceForm.result === 'ACCEPTED' ? '验收评价（可选）' : '修改意见'"
          prop="comment"
          :error="acceptanceServerErrors.comment"
        >
          <el-input
            v-model="acceptanceForm.comment"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <div class="actions">
          <el-button
            type="primary"
            native-type="submit"
            :loading="submittingAction === 'ACCEPTANCE'"
            :disabled="submittingAction !== null && submittingAction !== 'ACCEPTANCE'"
            >提交验收结果</el-button
          >
        </div>
      </el-form>
    </template>
  </el-card>
</template>

<style scoped>
.latest-delivery {
  display: grid;
  gap: 10px;
}
.history-delivery {
  display: grid;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #e5e7eb;
}
.history-delivery:last-child {
  border-bottom: 0;
}
.history-delivery p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.latest-delivery p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.section-heading span {
  color: #6b7280;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
</style>

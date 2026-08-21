<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Activity } from '@lucide/vue'
import { createProgress } from '@/api/progress'
import { getApiErrorMessage, getApiFieldErrors, getApiStatus } from '@/api/http'
import AppSectionHeader from '@/components/common/AppSectionHeader.vue'
import type {
  CreatedProgressResult,
  CreateProgressInput,
  RequestProgressSnapshot,
} from '@/types/progress'

const props = defineProps<{
  snapshot: RequestProgressSnapshot
}>()

const emit = defineEmits<{
  updated: [result: CreatedProgressResult]
  conflict: []
}>()

interface ProgressFormModel {
  progress: number
  content: string
  nextPlan: string
  nextUpdateAt: Date | null
  visibleToRequester: boolean
}

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = reactive<Record<string, string>>({})
const form = reactive<ProgressFormModel>({
  progress: 0,
  content: '',
  nextPlan: '',
  nextUpdateAt: null,
  visibleToRequester: true,
})

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const rules: FormRules = {
  progress: [
    {
      required: true,
      type: 'number',
      min: 0,
      max: 100,
      message: '进度必须为 0～100',
      trigger: 'change',
    },
    {
      validator: (_rule, _value, callback) => {
        if (!Number.isInteger(form.progress)) {
          callback(new Error('进度必须为整数'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  content: [
    {
      validator: (_rule, _value, callback) => {
        const length = form.content.trim().length
        if (length < 5 || length > 2000) {
          callback(new Error('进度说明应为 5～2000 个字符'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  nextPlan: [
    {
      validator: (_rule, _value, callback) => {
        if (form.nextPlan.trim().length > 2000) {
          callback(new Error('下一步计划不能超过 2000 个字符'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  nextUpdateAt: [
    {
      validator: (_rule, _value, callback) => {
        if (form.nextUpdateAt !== null && form.nextUpdateAt.getTime() <= Date.now()) {
          callback(new Error('预计下次更新时间必须晚于当前时间'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

function clearServerErrors(): void {
  Object.keys(serverErrors).forEach((key) => delete serverErrors[key])
}

function resetForm(): void {
  form.progress = props.snapshot.currentProgress
  form.content = ''
  form.nextPlan = ''
  form.nextUpdateAt = null
  form.visibleToRequester = true
  clearServerErrors()
  formRef.value?.clearValidate()
}

function buildInput(): CreateProgressInput {
  return {
    requestVersion: props.snapshot.requestVersion,
    progress: form.progress,
    content: form.content.trim(),
    nextPlan: form.nextPlan.trim() || null,
    nextUpdateAt: form.nextUpdateAt?.toISOString() ?? null,
    visibleToRequester: form.visibleToRequester,
  }
}

async function confirmSubmit(): Promise<boolean> {
  const decreasing = form.progress < props.snapshot.currentProgress

  try {
    await ElMessageBox.confirm(
      decreasing
        ? `新进度低于当前 ${props.snapshot.currentProgress}%，请确认这是一次进度修正。`
        : '确认发布这条进度记录吗？发布后记录不可修改。',
      decreasing ? '确认下调进度' : '确认发布进度',
      {
        type: decreasing ? 'warning' : 'info',
        confirmButtonText: '确认发布',
        cancelButtonText: '返回修改',
      },
    )
    return true
  } catch {
    return false
  }
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value || submitting.value || !props.snapshot.canUpdateProgress) return

  submitting.value = true
  clearServerErrors()

  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid || !(await confirmSubmit())) return

    const result = await createProgress(props.snapshot.requestId, buildInput())
    emit('updated', result)
  } catch (error) {
    if (getApiStatus(error) === 409) {
      ElMessage.warning('需求已被其他成员更新，正在重新加载最新数据')
      emit('conflict')
      return
    }

    Object.assign(serverErrors, getApiFieldErrors(error))
    ElMessage.error(getApiErrorMessage(error, '进度发布失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

function formatDateTime(value: string | null): string {
  if (!value) return '—'

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

watch(() => props.snapshot, resetForm, { immediate: true })
</script>

<template>
  <el-card>
    <template #header>
      <AppSectionHeader
        title="处理进度"
        description="当前完成度、进度记录与下一步计划"
        :icon="Activity"
      />
    </template>
    <div class="current-progress">
      <strong>当前进度</strong>
      <el-progress :percentage="snapshot.currentProgress" :stroke-width="12" />
      <span>最近更新：{{ formatDateTime(snapshot.lastProgressAt) }}</span>
      <span v-if="snapshot.nextUpdateAt">
        预计下次更新：{{ formatDateTime(snapshot.nextUpdateAt) }}
      </span>
    </div>

    <el-alert
      v-if="snapshot.needsFollowUp"
      type="warning"
      :closable="false"
      title="该需求长时间未更新或已超过预计更新时间，请及时跟进。"
    />

    <template v-if="snapshot.canUpdateProgress">
      <el-divider />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="完成进度" prop="progress" :error="serverErrors.progress">
          <el-input-number v-model="form.progress" :min="0" :max="100" :step="5" :precision="0" />
          <span class="percent-unit">%</span>
        </el-form-item>

        <el-form-item label="进度说明" prop="content" :error="serverErrors.content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="下一步计划（可选）" prop="nextPlan" :error="serverErrors.nextPlan">
          <el-input
            v-model="form.nextPlan"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item
          label="预计下次更新时间（可选）"
          prop="nextUpdateAt"
          :error="serverErrors.nextUpdateAt"
        >
          <el-date-picker
            v-model="form.nextUpdateAt"
            type="datetime"
            placeholder="请选择预计更新时间"
            class="full-width"
          />
        </el-form-item>

        <el-form-item label="可见范围" prop="visibleToRequester">
          <el-switch
            v-model="form.visibleToRequester"
            active-text="申请人可见"
            inactive-text="仅服务团队可见"
          />
        </el-form-item>

        <el-alert
          v-if="form.progress === 100"
          type="info"
          :closable="false"
          title="100% 仅代表完成程度；进入待验收仍需单独提交交付物。"
        />

        <div class="actions">
          <el-button @click="resetForm">清空本次填写</el-button>
          <el-button type="primary" native-type="submit" :loading="submitting">
            发布进度
          </el-button>
        </div>
      </el-form>
    </template>
  </el-card>
</template>

<style scoped>
.current-progress {
  display: grid;
  gap: 12px;
}

.percent-unit {
  margin-left: 8px;
}

.full-width {
  width: 100%;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
</style>

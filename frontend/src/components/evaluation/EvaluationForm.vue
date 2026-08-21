<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ClipboardPenLine } from '@lucide/vue'
import { createEvaluation } from '@/api/evaluations'
import { getApiErrorMessage, getApiFieldErrors, getApiStatus } from '@/api/http'
import AppSectionHeader from '@/components/common/AppSectionHeader.vue'
import type {
  CreatedEvaluationResult,
  CreateEvaluationInput,
  EvaluationConclusion,
} from '@/types/evaluation'

const props = defineProps<{
  requestId: string
  requestVersion: number
}>()

const emit = defineEmits<{
  submitted: [result: CreatedEvaluationResult]
  conflict: []
}>()

interface EvaluationFormModel {
  conclusion: EvaluationConclusion | ''
  publicComment: string
  solutionSummary: string
  estimatedWorkload: string
  estimatedFinishAt: Date | null
  requiredSkills: string
  risks: string
  internalNote: string
}

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = reactive<Record<string, string>>({})

const form = reactive<EvaluationFormModel>({
  conclusion: '',
  publicComment: '',
  solutionSummary: '',
  estimatedWorkload: '',
  estimatedFinishAt: null,
  requiredSkills: '',
  risks: '',
  internalNote: '',
})

const isFeasible = computed(() => form.conclusion === 'FEASIBLE')

const publicCommentLabel = computed(() => {
  if (form.conclusion === 'NEED_MORE_INFO') {
    return '需要补充的问题（申请人可见）'
  }

  if (form.conclusion === 'NOT_FEASIBLE') {
    return '暂不承接原因（申请人可见）'
  }

  return '评估说明（申请人可见）'
})

const rules: FormRules = {
  conclusion: [
    {
      required: true,
      message: '请选择评估结论',
      trigger: 'change',
    },
  ],
  publicComment: [
    {
      required: true,
      message: '请输入评估说明',
      trigger: 'blur',
    },
    {
      min: 10,
      max: 5000,
      message: '评估说明应为 10～5000 个字符',
      trigger: 'blur',
    },
  ],
  solutionSummary: [
    {
      validator: (_rule, _value, callback) => {
        if (!isFeasible.value) {
          callback()
          return
        }

        const length = form.solutionSummary.trim().length
        if (length < 10 || length > 5000) {
          callback(new Error('实施方案摘要应为 10～5000 个字符'))
          return
        }

        callback()
      },
      trigger: 'blur',
    },
  ],
  estimatedWorkload: [
    {
      validator: (_rule, _value, callback) => {
        if (!isFeasible.value) {
          callback()
          return
        }

        const text = form.estimatedWorkload.trim()

        if (!/^(?:0|[1-9]\d{0,5})(?:\.\d{1,2})?$/.test(text)) {
          callback(new Error('请输入大于 0、最多两位小数的人时数'))
          return
        }

        const value = Number(text)
        if (value <= 0 || value > 999999.99) {
          callback(new Error('预计工作量应在 0～999999.99 人时之间'))
          return
        }

        callback()
      },
      trigger: 'blur',
    },
  ],
  estimatedFinishAt: [
    {
      validator: (_rule, _value, callback) => {
        if (!isFeasible.value) {
          callback()
          return
        }

        if (
          !(form.estimatedFinishAt instanceof Date) ||
          form.estimatedFinishAt.getTime() <= Date.now()
        ) {
          callback(new Error('请选择晚于当前时间的预计完成时间'))
          return
        }

        callback()
      },
      trigger: 'change',
    },
  ],
  requiredSkills: [
    {
      max: 500,
      message: '所需技能不能超过 500 个字符',
      trigger: 'blur',
    },
  ],
  risks: [
    {
      max: 5000,
      message: '风险与依赖不能超过 5000 个字符',
      trigger: 'blur',
    },
  ],
  internalNote: [
    {
      max: 5000,
      message: '内部备注不能超过 5000 个字符',
      trigger: 'blur',
    },
  ],
}

function clearServerErrors() {
  for (const field of Object.keys(serverErrors)) {
    delete serverErrors[field]
  }
}

function disabledPastDate(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

watch(
  () => form.conclusion,
  (conclusion) => {
    if (conclusion !== 'FEASIBLE') {
      form.solutionSummary = ''
      form.estimatedWorkload = ''
      form.estimatedFinishAt = null

      formRef.value?.clearValidate(['solutionSummary', 'estimatedWorkload', 'estimatedFinishAt'])
    }
  },
)

function buildInput(): CreateEvaluationInput {
  const feasible = form.conclusion === 'FEASIBLE'

  return {
    requestVersion: props.requestVersion,
    conclusion: form.conclusion as EvaluationConclusion,
    publicComment: form.publicComment.trim(),
    solutionSummary: feasible ? form.solutionSummary.trim() : null,
    estimatedWorkload: feasible ? Number(form.estimatedWorkload.trim()) : null,
    estimatedFinishAt:
      feasible && form.estimatedFinishAt ? form.estimatedFinishAt.toISOString() : null,
    requiredSkills: form.requiredSkills.trim() || null,
    risks: form.risks.trim() || null,
    internalNote: form.internalNote.trim() || null,
  }
}

async function confirmNotFeasible(): Promise<boolean> {
  if (form.conclusion !== 'NOT_FEASIBLE') {
    return true
  }

  try {
    await ElMessageBox.confirm(
      '普通成员提交后将等待管理员确认；管理员提交时会直接驳回该需求。是否继续？',
      '确认评估结论',
      {
        type: 'warning',
        confirmButtonText: '继续提交',
        cancelButtonText: '返回修改',
      },
    )

    return true
  } catch {
    return false
  }
}

async function handleSubmit() {
  if (!formRef.value || submitting.value) {
    return
  }

  submitting.value = true
  clearServerErrors()

  try {
    const valid = await formRef.value.validate().catch(() => false)

    if (!valid || !(await confirmNotFeasible())) {
      return
    }

    const result = await createEvaluation(props.requestId, buildInput())

    emit('submitted', result)
    formRef.value.resetFields()
  } catch (error) {
    if (getApiStatus(error) === 409) {
      ElMessage.warning('需求已被其他成员更新，正在重新加载最新数据')
      emit('conflict')
      return
    }

    Object.assign(serverErrors, getApiFieldErrors(error))

    ElMessage.error(getApiErrorMessage(error, '评估提交失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-card>
    <template #header>
      <AppSectionHeader
        title="填写可行性评估"
        description="记录可行性、工作量、风险与建议方案"
        :icon="ClipboardPenLine"
      />
    </template>
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="评估结论" prop="conclusion" :error="serverErrors.conclusion">
        <el-radio-group v-model="form.conclusion">
          <el-radio-button value="FEASIBLE"> 可承接 </el-radio-button>
          <el-radio-button value="NEED_MORE_INFO"> 需补充资料 </el-radio-button>
          <el-radio-button value="NOT_FEASIBLE"> 暂不承接 </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item
        :label="publicCommentLabel"
        prop="publicComment"
        :error="serverErrors.publicComment"
      >
        <el-input
          v-model="form.publicComment"
          type="textarea"
          :rows="5"
          maxlength="5000"
          show-word-limit
        />
      </el-form-item>

      <template v-if="isFeasible">
        <el-form-item
          label="实施方案摘要"
          prop="solutionSummary"
          :error="serverErrors.solutionSummary"
        >
          <el-input
            v-model="form.solutionSummary"
            type="textarea"
            :rows="5"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <div class="evaluation-grid">
          <el-form-item
            label="预计工作量（人时）"
            prop="estimatedWorkload"
            :error="serverErrors.estimatedWorkload"
          >
            <el-input
              v-model="form.estimatedWorkload"
              type="number"
              min="0.01"
              step="0.01"
              placeholder="例如：16"
            />
          </el-form-item>

          <el-form-item
            label="预计完成时间"
            prop="estimatedFinishAt"
            :error="serverErrors.estimatedFinishAt"
          >
            <el-date-picker
              v-model="form.estimatedFinishAt"
              type="datetime"
              :disabled-date="disabledPastDate"
              placeholder="请选择日期和时间"
              class="full-width"
            />
          </el-form-item>
        </div>
      </template>

      <el-form-item
        label="所需技能（可选）"
        prop="requiredSkills"
        :error="serverErrors.requiredSkills"
      >
        <el-input
          v-model="form.requiredSkills"
          maxlength="500"
          placeholder="例如：Vue、Java、MySQL"
        />
      </el-form-item>

      <el-form-item label="风险与依赖（可选）" prop="risks" :error="serverErrors.risks">
        <el-input v-model="form.risks" type="textarea" :rows="3" maxlength="5000" show-word-limit />
      </el-form-item>

      <el-form-item
        label="服务团队内部备注（申请人不可见）"
        prop="internalNote"
        :error="serverErrors.internalNote"
      >
        <el-input
          v-model="form.internalNote"
          type="textarea"
          :rows="3"
          maxlength="5000"
          show-word-limit
        />
      </el-form-item>

      <div class="form-actions">
        <el-button type="primary" native-type="submit" :loading="submitting"> 提交评估 </el-button>
      </div>
    </el-form>
  </el-card>
</template>

<style scoped>
.evaluation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.full-width {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .evaluation-grid {
    grid-template-columns: 1fr;
  }
}
</style>

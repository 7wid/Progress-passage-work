<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { UsersRound } from '@lucide/vue'
import { getAssignableMemberOptions, updateRequestAssignment } from '@/api/assignments'
import { getApiErrorMessage, getApiFieldErrors, getApiStatus } from '@/api/http'
import AppSectionHeader from '@/components/common/AppSectionHeader.vue'
import type { AssignableMemberOption, RequestAssignment } from '@/types/assignment'

const props = defineProps<{
  assignment: RequestAssignment
  isAdmin: boolean
}>()

const emit = defineEmits<{
  updated: [assignment: RequestAssignment]
  conflict: []
}>()

interface AssignmentFormModel {
  ownerId: string
  participantIds: string[]
  reason: string
}

const formRef = ref<FormInstance>()
const submitting = ref(false)
const optionsLoading = ref(false)
const optionsError = ref('')
const options = ref<AssignableMemberOption[]>([])
const serverErrors = reactive<Record<string, string>>({})
const form = reactive<AssignmentFormModel>({ ownerId: '', participantIds: [], reason: '' })

const canEdit = computed(
  () =>
    props.isAdmin &&
    (props.assignment.requestStatus === 'PENDING_ASSIGNMENT' ||
      props.assignment.requestStatus === 'IN_PROGRESS'),
)
const participantOptions = computed(() =>
  options.value.filter((option) => option.id !== form.ownerId),
)

const rules: FormRules = {
  ownerId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
  participantIds: [
    { type: 'array', max: 20, message: '参与成员不能超过 20 人', trigger: 'change' },
  ],
  reason: [
    { required: true, message: '请输入调整原因', trigger: 'blur' },
    { min: 5, max: 500, message: '调整原因应为 5～500 个字符', trigger: 'blur' },
  ],
}

function resetForm(): void {
  form.ownerId = props.assignment.owner?.userId ?? ''
  form.participantIds = props.assignment.participants.map((member) => member.userId)
  form.reason = ''
  formRef.value?.clearValidate()
}

function clearServerErrors(): void {
  Object.keys(serverErrors).forEach((key) => delete serverErrors[key])
}

function optionLabel(option: AssignableMemberOption): string {
  return `${option.displayName}（${option.account}）`
}

function roleLabel(role: 'MEMBER' | 'ADMIN'): string {
  return role === 'ADMIN' ? '管理员' : '服务团队成员'
}

function formatDateTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '—'
    : new Intl.DateTimeFormat('zh-CN', {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(date)
}

async function loadOptions(): Promise<void> {
  if (!canEdit.value || optionsLoading.value) return
  optionsLoading.value = true
  optionsError.value = ''
  try {
    options.value = await getAssignableMemberOptions()
  } catch (error) {
    optionsError.value = getApiErrorMessage(error, '候选成员加载失败')
  } finally {
    optionsLoading.value = false
  }
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value || submitting.value) return
  clearServerErrors()
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm(
      props.assignment.owner
        ? '确认保存负责人和参与成员的调整吗？'
        : '确认分配后，需求将进入“处理中”。是否继续？',
      props.assignment.owner ? '调整任务成员' : '首次分配任务',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '返回修改' },
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const result = await updateRequestAssignment(props.assignment.requestId, {
      requestVersion: props.assignment.requestVersion,
      ownerId: form.ownerId,
      participantIds: form.participantIds,
      reason: form.reason,
    })
    emit('updated', result)
  } catch (error) {
    if (getApiStatus(error) === 409) {
      ElMessage.warning('任务成员已被其他管理员更新，请使用最新数据重试')
      emit('conflict')
      return
    }
    Object.assign(serverErrors, getApiFieldErrors(error))
    ElMessage.error(getApiErrorMessage(error, '任务成员更新失败'))
  } finally {
    submitting.value = false
  }
}

watch(() => props.assignment, resetForm, { immediate: true })
watch(
  () => form.ownerId,
  (ownerId) => {
    form.participantIds = form.participantIds.filter((id) => id !== ownerId)
  },
)
watch(
  canEdit,
  (editable) => {
    if (editable && options.value.length === 0) void loadOptions()
  },
  { immediate: true },
)
</script>

<template>
  <el-card>
    <template #header>
      <AppSectionHeader
        title="任务成员"
        description="负责人、参与成员与分配调整"
        :icon="UsersRound"
        tone="purple"
      />
    </template>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="主负责人">
        <template v-if="assignment.owner">
          {{ assignment.owner.displayName }}
          <el-tag size="small">{{ roleLabel(assignment.owner.role) }}</el-tag>
        </template>
        <span v-else>尚未分配</span>
      </el-descriptions-item>
      <el-descriptions-item label="参与成员">
        <div v-if="assignment.participants.length" class="member-tags">
          <el-tag v-for="member in assignment.participants" :key="member.id" type="info">
            {{ member.displayName }}
          </el-tag>
        </div>
        <span v-else>暂无参与成员</span>
      </el-descriptions-item>
      <el-descriptions-item v-if="assignment.owner" label="负责人加入时间">
        {{ formatDateTime(assignment.owner.joinedAt) }}
      </el-descriptions-item>
    </el-descriptions>

    <template v-if="canEdit">
      <el-divider />
      <el-alert v-if="optionsError" type="error" :closable="false" :title="optionsError">
        <template #default>
          <el-button link type="primary" @click="loadOptions">重新加载候选成员</el-button>
        </template>
      </el-alert>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="主负责人" prop="ownerId" :error="serverErrors.ownerId">
          <el-select
            v-model="form.ownerId"
            filterable
            :loading="optionsLoading"
            placeholder="请选择负责人"
            class="full-width"
          >
            <el-option
              v-for="option in options"
              :key="option.id"
              :label="optionLabel(option)"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="参与成员（最多 20 人）"
          prop="participantIds"
          :error="serverErrors.participantIds"
        >
          <el-select
            v-model="form.participantIds"
            multiple
            filterable
            collapse-tags
            :max-collapse-tags="3"
            :loading="optionsLoading"
            placeholder="可不选择参与成员"
            class="full-width"
          >
            <el-option
              v-for="option in participantOptions"
              :key="option.id"
              :label="optionLabel(option)"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分配或调整原因" prop="reason" :error="serverErrors.reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <div class="actions">
          <el-button @click="resetForm">恢复当前配置</el-button>
          <el-button type="primary" native-type="submit" :loading="submitting">
            保存任务成员
          </el-button>
        </div>
      </el-form>
    </template>
  </el-card>
</template>

<style scoped>
.member-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.full-width {
  width: 100%;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

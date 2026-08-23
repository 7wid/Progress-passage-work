<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Check, Sparkles, UsersRound } from '@lucide/vue'
import { getRequestMemberRecommendations, updateRequestAssignment } from '@/api/assignments'
import { getApiErrorMessage, getApiFieldErrors, getApiStatus } from '@/api/http'
import AppSectionHeader from '@/components/common/AppSectionHeader.vue'
import type {
  AssignableMemberOption,
  MemberRecommendation,
  MemberRecommendationResult,
  RequestAssignment,
} from '@/types/assignment'

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
const recommendationResult = ref<MemberRecommendationResult | null>(null)
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
const recommendations = computed(() => recommendationResult.value?.members.slice(0, 3) ?? [])
const recommendationById = computed(
  () =>
    new Map(
      (recommendationResult.value?.members ?? []).map((candidate) => [candidate.id, candidate]),
    ),
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
  const recommendation = recommendationById.value.get(option.id)
  const loadLabel = recommendation ? ` · 预计 ${recommendation.projectedActiveRequestCount} 项` : ''
  return `${option.displayName}（${option.account}）${loadLabel}`
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
    const result = await getRequestMemberRecommendations(props.assignment.requestId)
    recommendationResult.value = result
    options.value = result.members.map(({ id, account, displayName, role }) => ({
      id,
      account,
      displayName,
      role,
    }))
  } catch (error) {
    recommendationResult.value = null
    options.value = []
    optionsError.value = getApiErrorMessage(error, '候选成员加载失败')
  } finally {
    optionsLoading.value = false
  }
}

function selectRecommendation(candidate: MemberRecommendation): void {
  form.ownerId = candidate.id
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
      <section class="recommendations" aria-labelledby="member-recommendations-title">
        <header class="recommendations__header">
          <span class="recommendations__icon" aria-hidden="true">
            <Sparkles :size="18" />
          </span>
          <div>
            <h3 id="member-recommendations-title">成员推荐</h3>
            <p>综合最新可承接评估的技能要求与分配后的预计主责任务数排序。</p>
          </div>
        </header>

        <div class="recommendation-context">
          <span>本次技能要求</span>
          <strong>{{
            optionsLoading
              ? '正在读取最新评估…'
              : recommendationResult?.requiredSkills || '未记录，主要按预计负载排序'
          }}</strong>
        </div>

        <p v-if="optionsLoading" class="recommendation-status" role="status">正在分析候选成员…</p>
        <ol v-else-if="recommendations.length" class="recommendation-list">
          <li
            v-for="candidate in recommendations"
            :key="candidate.id"
            class="recommendation-card"
            :class="{ 'is-selected': form.ownerId === candidate.id }"
          >
            <div class="recommendation-card__topline">
              <span class="recommendation-rank">推荐 {{ candidate.rank }}</span>
              <el-tag size="small" effect="plain">{{ roleLabel(candidate.role) }}</el-tag>
            </div>
            <h4>{{ candidate.displayName }}</h4>
            <p class="recommendation-account">{{ candidate.account }}</p>

            <dl class="recommendation-metrics">
              <div>
                <dt>当前主责任务</dt>
                <dd>{{ candidate.activeRequestCount }} 项</dd>
              </div>
              <div>
                <dt>分配后预计</dt>
                <dd>{{ candidate.projectedActiveRequestCount }} 项</dd>
              </div>
            </dl>

            <p v-if="candidate.matchedSkills.length" class="match-summary">
              <Check :size="16" aria-hidden="true" />
              匹配技能：{{ candidate.matchedSkills.join('、') }}
            </p>
            <p v-else class="match-summary is-muted">暂未匹配技能标签，按预计负载排序。</p>

            <div v-if="candidate.skills.length" class="skill-tags" aria-label="成员技能">
              <el-tag
                v-for="skill in candidate.skills"
                :key="skill"
                size="small"
                :type="candidate.matchedSkills.includes(skill) ? 'success' : 'info'"
                effect="light"
              >
                {{ skill }}{{ candidate.matchedSkills.includes(skill) ? ' · 匹配' : '' }}
              </el-tag>
            </div>
            <p v-else class="skill-empty">尚未维护技能标签</p>

            <el-button
              class="recommendation-action"
              type="primary"
              plain
              :aria-pressed="form.ownerId === candidate.id"
              @click="selectRecommendation(candidate)"
            >
              <Check v-if="form.ownerId === candidate.id" :size="16" aria-hidden="true" />
              {{ form.ownerId === candidate.id ? '当前已选择' : '选为负责人' }}
            </el-button>
          </li>
        </ol>
        <p v-else-if="!optionsError" class="recommendation-status">
          暂无可分配的在岗成员，请先在成员管理中启用账号。
        </p>
      </section>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="主负责人" prop="ownerId" :error="serverErrors.ownerId">
          <p class="form-helper">推荐结果仅用于辅助判断，管理员仍可选择任意在岗成员。</p>
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

.recommendations {
  margin-bottom: 24px;
  padding: 16px;
  background: var(--color-surface-secondary);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
}

.recommendations__header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.recommendations__icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: var(--color-primary-strong);
  background: var(--color-primary-soft);
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-md);
}

.recommendations h3,
.recommendation-card h4,
.recommendations p {
  margin: 0;
}

.recommendations h3 {
  color: var(--color-text-primary);
  font-size: 16px;
  line-height: 1.5;
}

.recommendations__header p {
  margin-top: 2px;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.recommendation-context {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  gap: 8px 12px;
  align-items: start;
  margin-top: 12px;
  padding: 10px 12px;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  font-size: 13px;
  line-height: 1.5;
}

.recommendation-context strong {
  min-width: 0;
  color: var(--color-text-primary);
  overflow-wrap: anywhere;
}

.recommendation-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.recommendation-card {
  display: flex;
  min-width: 0;
  padding: 14px;
  flex-direction: column;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition:
    border-color var(--motion-base) var(--ease-standard),
    box-shadow var(--motion-base) var(--ease-standard),
    background-color var(--motion-base) var(--ease-standard);
}

.recommendation-card:hover {
  border-color: var(--color-primary-border);
  box-shadow: var(--shadow-raised);
}

.recommendation-card.is-selected {
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
}

.recommendation-card__topline {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.recommendation-rank {
  color: var(--color-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.recommendation-card h4 {
  margin-top: 12px;
  color: var(--color-text-primary);
  font-size: 16px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.recommendation-account {
  margin-top: 2px !important;
  color: var(--color-text-tertiary);
  font-family: SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.recommendation-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 14px 0 0;
}

.recommendation-metrics div {
  min-width: 0;
  padding: 8px;
  background: var(--color-surface-secondary);
  border-radius: var(--radius-sm);
}

.recommendation-metrics dt {
  color: var(--color-text-tertiary);
  font-size: 12px;
  line-height: 1.4;
}

.recommendation-metrics dd {
  margin: 3px 0 0;
  color: var(--color-text-primary);
  font-size: 15px;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}

.match-summary {
  display: flex;
  gap: 6px;
  align-items: flex-start;
  min-height: 42px;
  margin-top: 12px !important;
  color: var(--color-success);
  font-size: 12px;
  line-height: 1.5;
}

.match-summary svg {
  flex: 0 0 auto;
  margin-top: 1px;
}

.match-summary.is-muted,
.skill-empty {
  color: var(--color-text-tertiary);
}

.skill-tags {
  display: flex;
  min-height: 24px;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.skill-empty {
  min-height: 24px;
  margin-top: 10px !important;
  font-size: 12px;
  line-height: 1.5;
}

.recommendation-action {
  width: 100%;
  min-height: 44px;
  margin-top: 14px;
}

.recommendation-status {
  margin-top: 16px !important;
  padding: 20px;
  color: var(--color-text-secondary);
  text-align: center;
}

.form-helper {
  margin: -4px 0 8px;
  color: var(--color-text-tertiary);
  font-size: 13px;
  line-height: 1.5;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 900px) {
  .recommendation-list {
    grid-template-columns: 1fr;
  }

  .match-summary {
    min-height: auto;
  }
}

@media (max-width: 480px) {
  .recommendations {
    padding: 12px;
  }

  .recommendation-context {
    grid-template-columns: 1fr;
  }

  .actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .actions :deep(.el-button) {
    width: 100%;
    min-height: 44px;
    margin-left: 0;
  }
}
</style>

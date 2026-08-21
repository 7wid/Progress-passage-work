<script setup lang="ts">
import { computed } from 'vue'
import { ClipboardCheck } from '@lucide/vue'
import EvaluationConclusionTag from './EvaluationConclusionTag.vue'
import AppSectionHeader from '@/components/common/AppSectionHeader.vue'
import type { EvaluationRecord } from '@/types/evaluation'

const props = withDefaults(
  defineProps<{
    evaluations: EvaluationRecord[]
    confirmableEvaluationId?: string | null
    confirmingEvaluationId?: string | null
  }>(),
  {
    confirmableEvaluationId: null,
    confirmingEvaluationId: null,
  },
)

const emit = defineEmits<{
  confirmRejection: [evaluation: EvaluationRecord]
}>()

const orderedEvaluations = computed(() =>
  [...props.evaluations].sort((left, right) => right.version - left.version),
)

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const numberFormatter = new Intl.NumberFormat('zh-CN', {
  maximumFractionDigits: 2,
})

function formatDateTime(value: string | null): string {
  if (!value) return '—'

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}
</script>

<template>
  <el-card>
    <template #header>
      <AppSectionHeader
        title="可行性评估记录"
        description="技术判断与历史评估版本"
        :icon="ClipboardCheck"
        tone="orange"
      />
    </template>
    <el-empty v-if="orderedEvaluations.length === 0" description="暂无评估记录" />

    <div v-else class="evaluation-list">
      <article
        v-for="evaluation in orderedEvaluations"
        :key="evaluation.id"
        class="evaluation-item"
      >
        <div class="evaluation-item__header">
          <div class="evaluation-item__title">
            <EvaluationConclusionTag :conclusion="evaluation.conclusion" />
            <strong>第 {{ evaluation.version }} 次评估</strong>
          </div>

          <span>
            {{ evaluation.evaluatorName }} ·
            {{ formatDateTime(evaluation.createdAt) }}
          </span>
        </div>

        <section>
          <h4>评估说明（申请人可见）</h4>
          <p class="pre-wrap">
            {{ evaluation.publicComment }}
          </p>
        </section>

        <el-descriptions v-if="evaluation.conclusion === 'FEASIBLE'" :column="2" border>
          <el-descriptions-item label="预计工作量">
            {{
              evaluation.estimatedWorkload === null
                ? '—'
                : `${numberFormatter.format(evaluation.estimatedWorkload)} 人时`
            }}
          </el-descriptions-item>

          <el-descriptions-item label="预计完成时间">
            {{ formatDateTime(evaluation.estimatedFinishAt) }}
          </el-descriptions-item>

          <el-descriptions-item label="实施方案摘要" :span="2">
            <span class="pre-wrap">
              {{ evaluation.solutionSummary ?? '—' }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <section v-if="evaluation.requiredSkills">
          <h4>所需技能</h4>
          <p class="pre-wrap">
            {{ evaluation.requiredSkills }}
          </p>
        </section>

        <section v-if="evaluation.risks">
          <h4>风险与依赖</h4>
          <p class="pre-wrap">
            {{ evaluation.risks }}
          </p>
        </section>

        <el-alert
          v-if="evaluation.internalNote"
          type="info"
          :closable="false"
          title="服务团队内部备注"
        >
          <template #default>
            <span class="pre-wrap">
              {{ evaluation.internalNote }}
            </span>
          </template>
        </el-alert>

        <div v-if="evaluation.id === confirmableEvaluationId" class="evaluation-item__actions">
          <el-button
            type="danger"
            :loading="confirmingEvaluationId === evaluation.id"
            @click="emit('confirmRejection', evaluation)"
          >
            确认不承接并驳回
          </el-button>
        </div>
      </article>
    </div>
  </el-card>
</template>

<style scoped>
.evaluation-list {
  display: grid;
  gap: 16px;
}

.evaluation-item {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: #fbfcfe;
  transition:
    border-color var(--motion-fast) ease,
    box-shadow var(--motion-fast) ease;
}

.evaluation-item:hover {
  border-color: var(--color-primary-border);
  box-shadow: 0 6px 18px rgb(15 23 42 / 5%);
}

.evaluation-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--color-text-tertiary);
}

.evaluation-item__title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text-primary);
}

h4 {
  margin: 0 0 8px;
  color: var(--color-text-primary);
  font-size: 13px;
}

p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.pre-wrap {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.evaluation-item__actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .evaluation-item__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

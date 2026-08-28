<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ClipboardList } from '@lucide/vue'
import { getEvaluations } from '@/api/evaluations'
import type { EvaluationRecord } from '@/types/evaluation'

const props = defineProps<{ requestId: string }>()
const loading = ref(true)
const failed = ref(false)
const latest = ref<EvaluationRecord | null>(null)
let loadSequence = 0

async function loadGuide() {
  const sequence = ++loadSequence
  const id = props.requestId
  loading.value = true
  failed.value = false
  latest.value = null
  try {
    const evaluations = await getEvaluations(id)
    if (sequence !== loadSequence) return
    const newest = [...evaluations].sort((left, right) => right.version - left.version)[0]
    // Never surface an older request for information after a newer conclusion.
    latest.value = newest?.conclusion === 'NEED_MORE_INFO' ? newest : null
  } catch {
    if (sequence === loadSequence) failed.value = true
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

watch(() => props.requestId, loadGuide, { immediate: true })
onBeforeUnmount(() => {
  loadSequence++
})
</script>

<template>
  <section class="supplement-guide" aria-labelledby="supplement-guide-title" :aria-busy="loading">
    <div class="supplement-guide__heading">
      <ClipboardList :size="20" aria-hidden="true" />
      <div>
        <h2 id="supplement-guide-title">本次需要补充什么</h2>
        <p>请对照以下说明修改对应字段，保存不会重新进入评估，完成后还需提交补充资料。</p>
      </div>
    </div>
    <p v-if="loading" role="status">正在加载补充要求…</p>
    <div v-else-if="failed" role="alert" class="supplement-guide__feedback">
      <p>补充要求加载失败，已填写的内容不会受影响。请重试，或到需求详情查看评估记录。</p>
      <el-button @click="loadGuide">重新加载补充要求</el-button>
    </div>
    <div v-else-if="latest" class="supplement-guide__comment">
      <span>第 {{ latest.version }} 次评估 · {{ latest.evaluatorName }}</span>
      <p>{{ latest.publicComment }}</p>
    </div>
    <p v-else role="status">
      暂无可展示的最新补充要求，请到需求详情核对评估记录，或联系服务团队确认。
    </p>
    <RouterLink :to="{ name: 'request-detail', params: { id: requestId } }">
      查看需求详情与评估记录
    </RouterLink>
  </section>
</template>

<style scoped>
.supplement-guide {
  box-sizing: border-box;
  padding: 20px 24px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-text-primary);
  background: var(--color-primary-soft);
  overflow-wrap: anywhere;
}

.supplement-guide__heading {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.supplement-guide__heading > svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--color-primary-strong);
}

.supplement-guide h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
}

.supplement-guide p {
  margin: 8px 0 12px;
  line-height: 1.7;
}

.supplement-guide__heading p,
.supplement-guide__comment > span {
  color: var(--color-text-secondary);
  font-size: 13px;
}

.supplement-guide__comment {
  margin: 4px 0 12px;
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
}

.supplement-guide__comment p {
  margin-bottom: 0;
  white-space: pre-wrap;
}

.supplement-guide__feedback {
  margin-bottom: 12px;
}

.supplement-guide a {
  color: var(--color-primary-strong);
  font-size: 13px;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.supplement-guide a:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 4px;
}

@media (max-width: 768px) {
  .supplement-guide {
    padding: 18px 16px;
  }
}
</style>

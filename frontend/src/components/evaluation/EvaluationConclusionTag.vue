<script setup lang="ts">
import { computed } from 'vue'
import type { EvaluationConclusion } from '@/types/evaluation'

const props = defineProps<{
  conclusion: EvaluationConclusion
}>()

const conclusionMap: Record<
  EvaluationConclusion,
  {
    label: string
    type: 'success' | 'warning' | 'danger'
  }
> = {
  FEASIBLE: {
    label: '可承接',
    type: 'success',
  },
  NEED_MORE_INFO: {
    label: '需补充资料',
    type: 'warning',
  },
  NOT_FEASIBLE: {
    label: '暂不承接',
    type: 'danger',
  },
}

const display = computed(() => conclusionMap[props.conclusion])
</script>

<template>
  <el-tag :type="display.type">
    {{ display.label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RequestStatus } from '@/types/request'

const props = defineProps<{ status: RequestStatus }>()

const statusMap: Record<
  RequestStatus,
  { label: string; type: 'info' | 'primary' | 'warning' | 'success' | 'danger' }
> = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING_REVIEW: { label: '待评估', type: 'primary' },
  NEED_MORE_INFO: { label: '待补充', type: 'warning' },
  PENDING_ASSIGNMENT: { label: '待分配', type: 'primary' },
  IN_PROGRESS: { label: '处理中', type: 'primary' },
  PENDING_ACCEPTANCE: { label: '待验收', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' },
}

const display = computed(() => statusMap[props.status])
</script>

<template>
  <el-tag :type="display.type">{{ display.label }}</el-tag>
</template>
